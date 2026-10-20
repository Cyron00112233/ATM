package com.atm.service.impl;

import com.atm.common.BusinessException;
import com.atm.common.ResultCode;
import com.atm.dto.*;
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
import com.atm.dto.WithdrawRequest;
import com.atm.dto.WithdrawResponse;
import com.atm.entity.Account;
import com.atm.entity.AtmCashBox;
import com.atm.entity.TransRecord;
import com.atm.mapper.AccountMapper;
import com.atm.mapper.AtmCashBoxMapper;
import com.atm.mapper.TransRecordMapper;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

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
    private final AccountMapper accountMapper;
    private final AtmCashBoxMapper cashBoxMapper;
    private final TransRecordMapper transRecordMapper;

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
    //--------------------4.取款----------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdraw(WithdrawRequest request) {

        //1.校验会话
        AtmSession session = getValidSession(request.getSessionToken());

        //2.查卡 + 状态
        BankCard card = bankCardMapper.selectByCardNo(session.getCardNo());
        if(card == null){
            throw new BusinessException(ResultCode.CARD_NOT_FOUND);
        }
        checkCardStatus(card);

        //3.金额校验
        BigDecimal amount = request.getAmount();
        validateAmount(amount);

        //4.查账户
        Account account = accountMapper.selectByCardId(card.getId());
        if(account == null){
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if(account.getStatus() == null || account.getStatus() != 1){
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND,"账户状态异常，无法取款");
        }

        //5.扣款：余额不足时返回0行
        int rows = accountMapper.decreaseBalance(account.getId(),amount);
        if(rows == 0){
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        //6.配钞：只支持100元卷
        int need100 = amount.intValue() / 100;
        AtmCashBox box100 = cashBoxMapper.selectByDeviceAndDenomination(session.getDeviceNo(),100);
        if(box100 == null || box100.getNoteCount() < need100){
            //抛异常 -> 事务回滚 -> 第五步扣的钱被撤销
            throw new BusinessException(ResultCode.CASH_NOT_ENOUGH,"ATM 现金不足，请换台机器或改取其他金额");
        }
        cashBoxMapper.decreaseNoteCount(box100.getId(),need100);

        //7.记流水
        String transNo = generateTransNo();
        TransRecord record = new TransRecord();
        record.setTransNo(transNo);
        record.setTransTypeCode("WITHDRAW");
        record.setCardNo(card.getCardNo());
        record.setAccountId(account.getId());
        record.setAmount(amount);
        record.setFee(BigDecimal.ZERO);
        record.setBeforeBalance(account.getAvailableBalance());
        record.setAfterBalance(account.getAvailableBalance().subtract(amount));
        record.setCurrency("CNY");
        record.setStatus(1);
        record.setDeviceNo(session.getDeviceNo());
        record.setSessionId(session.getId());
        record.setRemark("ATM取款");
        transRecordMapper.insert(record);

        //8.刷新会话活动时间
        atmSessionMapper.updateLastActive(session.getId());

        log.info("取款成功 tranNO={},cardNo={},amount={}",transNo,card.getCardNo(),amount);

        //9.组装响应
        WithdrawResponse response = new WithdrawResponse();
        response.setTransNo(transNo);
        response.setAmount(amount);
        response.setBalanceAfter(record.getAfterBalance());
        response.setCashDetail("100 元 x"+need100+"张");
        response.setMessage("请取走现金");
        return response;
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

    //金额校验：必须是100的整数倍，单笔100-5000
    private void validateAmount(BigDecimal amount){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new BusinessException(ResultCode.AMOUNT_ILLEGAL);
        }
        if (amount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ResultCode.AMOUNT_ILLEGAL, "取款金额必须是 100 的整数倍");
        }
        if(amount.compareTo(new BigDecimal("5000"))>0){
            throw new BusinessException(ResultCode.AMOUNT_LIMIT_EXCEED,"单笔取款最多5000元");
        }
    }

    //生成胶衣流水号：14 位时间 + 6位随机
    private String generateTransNo(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                +String.format("%06d",ThreadLocalRandom.current().nextInt(1000000));
    }
}
