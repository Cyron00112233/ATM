package com.atm.service.impl;

import com.atm.common.BusinessException;
import com.atm.common.ResultCode;
import com.atm.dto.InsertCardRequest;
import com.atm.dto.InsertCardResponse;
import com.atm.dto.VerifyPasswordRequest;
import com.atm.entity.AtmSession;
import com.atm.entity.BankCard;
import com.atm.entity.Customer;
import com.atm.mapper.AtmSessionMapper;
import com.atm.mapper.BankCardMapper;
import com.atm.mapper.CustomerMapper;
import com.atm.service.AtmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ATM 端业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtmServiceImpl implements AtmService {

    /** 密码最多允许错误次数 */
    private static final int MAX_PWD_ERROR = 3;

    /** 会话超时时间（分钟） */
    private static final int SESSION_TIMEOUT_MINUTES = 5;

    /** 默认机具编号 */
    private static final String DEFAULT_DEVICE_NO = "ATM001";

    private final BankCardMapper bankCardMapper;
    private final AtmSessionMapper atmSessionMapper;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------------- 1. 插卡 ----------------
    @Override
    public InsertCardResponse insertCard(InsertCardRequest request) {

        // 1.查卡
        BankCard card = bankCardMapper.selectByCardNo(request.getCardNo());
        if (card == null) {
            throw new BusinessException(ResultCode.CARD_NOT_FOUND);
        }

        // 2.卡状态检查（正常/锁定/挂失/注销）
        checkCardStatus(card);

        // 3.有效期检查（为 null 表示长期有效）
        if (card.getExpireDate() != null && card.getExpireDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.CARD_STATUS_ABNORMAL, "卡片已过有效期，请更换新卡");
        }

        // 4.查客户（为了显示姓名）
        Customer customer = customerMapper.selectById(card.getCustomerId());

        // 5.生成会话
        AtmSession session = new AtmSession();
        session.setSessionNo(UUID.randomUUID().toString().replace("-", ""));
        session.setToken(UUID.randomUUID().toString().replace("-", ""));
        session.setCardNo(card.getCardNo());
        session.setDeviceNo(request.getDeviceNo() == null || request.getDeviceNo().isBlank()
                ? DEFAULT_DEVICE_NO : request.getDeviceNo());
        session.setStatus(1);              // 1 = 进行中
        atmSessionMapper.insert(session);  // 插入后 session.getId() 会被自动回填

        log.info("插卡成功 cardNo={}, sessionNo={}", card.getCardNo(), session.getSessionNo());

        // 6.组装响应（卡号脱敏）
        InsertCardResponse response = new InsertCardResponse();
        response.setSessionToken(session.getToken());
        response.setCardNo(maskCardNo(card.getCardNo()));
        response.setCustomerName(customer == null ? "" : customer.getRealName());
        response.setMessage("插卡成功，请输入 6 位交易密码");
        return response;
    }

    // ---------------- 2. 验密码 ----------------
    // ⚠️ 这里故意不加 @Transactional，否则抛业务异常会把"错误次数+1""锁卡"一起回滚
    @Override
    public void verifyPassword(VerifyPasswordRequest request) {

        // 1.校验会话（存在/进行中/未超时）
        AtmSession session = getValidSession(request.getSessionToken());

        // 2.查卡
        BankCard card = bankCardMapper.selectByCardNo(session.getCardNo());
        if (card == null) {
            throw new BusinessException(ResultCode.CARD_NOT_FOUND);
        }

        // 3.卡状态检查
        checkCardStatus(card);

        // 4.BCrypt 校验
        if (!passwordEncoder.matches(request.getPassword(), card.getPassword())) {

            // 错误次数原子 +1
            bankCardMapper.increasePwdErrorCount(card.getId());

            // 重新读一次，拿到最新错误次数
            int errorCount = bankCardMapper.selectByCardNo(card.getCardNo()).getPwdErrorCount();

            // 达到三次 -> 锁卡
            if (errorCount >= MAX_PWD_ERROR) {
                bankCardMapper.lockCard(card.getId());
                log.warn("密码连续错误 {} 次，卡片已锁定 cardNo={}", errorCount, card.getCardNo());
                throw new BusinessException(ResultCode.CARD_LOCKED,
                        "密码连续错误 " + MAX_PWD_ERROR + " 次，卡片已锁定，请联系银行");
            }

            int remain = MAX_PWD_ERROR - errorCount;
            log.warn("密码错误 cardNo={}, 剩余尝试 {} 次", card.getCardNo(), remain);
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "密码错误，还可尝试 " + remain + " 次");
        }

        // 5.验证成功 -> 错误次数归零
        bankCardMapper.resetPwdErrorCount(card.getId());

        // 6.刷新最后活动时间
        atmSessionMapper.updateLastActive(session.getId());

        log.info("密码验证通过 cardNo={}", card.getCardNo());
    }

    // ---------------- 3. 退卡 ----------------
    @Override
    public void ejectCard(String sessionToken) {

        AtmSession session = getValidSession(sessionToken);

        // status = 2 -> 已退卡，同时记录 logout_time
        atmSessionMapper.updateStatus(session.getId(), 2);

        log.info("退卡完成 sessionNo={}", session.getSessionNo());
    }

    // ---------------- 私有方法 ----------------

    /** 校验卡状态，不正常就抛异常 */
    private void checkCardStatus(BankCard card) {
        Integer status = card.getCardStatus();
        if (status == null) {
            throw new BusinessException(ResultCode.CARD_STATUS_ABNORMAL);
        }
        switch (status) {
            case 1 -> { /* 正常，放行 */ }
            case 2 -> throw new BusinessException(ResultCode.CARD_LOCKED,
                    "卡片已锁定" + (card.getLockTime() == null ? "" : "（" + card.getLockTime() + "）")
                            + "，请联系银行解锁");
            case 3 -> throw new BusinessException(ResultCode.CARD_STATUS_ABNORMAL, "卡片已挂失");
            case 4 -> throw new BusinessException(ResultCode.CARD_STATUS_ABNORMAL, "卡片已注销");
            default -> throw new BusinessException(ResultCode.CARD_STATUS_ABNORMAL);
        }
    }

    /** 根据 token 取会话并校验：存在、进行中、未超时 */
    private AtmSession getValidSession(String token) {

        AtmSession session = atmSessionMapper.selectByToken(token);
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "会话不存在，请重新插卡");
        }
        if (session.getStatus() == null || session.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "会话已结束，请重新插卡");
        }

        // 超时检查
        LocalDateTime last = session.getLastActiveTime() == null
                ? session.getLoginTime() : session.getLastActiveTime();
        if (last != null && last.plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            atmSessionMapper.updateStatus(session.getId(), 3);   // 3 = 超时
            throw new BusinessException(ResultCode.UNAUTHORIZED,
                    "会话已超时（" + SESSION_TIMEOUT_MINUTES + " 分钟无操作），请重新插卡");
        }
        return session;
    }

    /** 卡号脱敏：保留前四位和后四位，中间打星号 */
    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return cardNo;
        }
        return cardNo.substring(0, 4)
                + "*".repeat(cardNo.length() - 8)
                + cardNo.substring(cardNo.length() - 4);
    }
}
