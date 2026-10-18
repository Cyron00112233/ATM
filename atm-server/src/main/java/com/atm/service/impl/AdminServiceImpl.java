package com.atm.service.impl;

import com.atm.common.BusinessException;
import com.atm.common.ResultCode;
import com.atm.dto.LoginRequest;
import com.atm.dto.LoginResponse;
import com.atm.entity.SysUser;
import com.atm.mapper.SysUserMapper;
import com.atm.service.AdminService;
import com.atm.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    @Override
    public LoginResponse login(LoginRequest request) {

        //1.按用户名查管理员
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());

        //2.用户不存在 —— 提示词故意和“密码错误”保持一致，防止被人探测出哪些账号存在
        if(user == null){
            log.warn("登录失败：账号不存在 -> {}",request.getUsername());
            throw new BusinessException(ResultCode.PASSWORD_ERROR,"用户名或密码错误");
        }

        //3.账号被禁用
        if(user.getStatus() == null || user.getStatus() != 1){
            log.warn("登录失败：账号被禁用 -> {}",request.getUsername());
            throw new BusinessException(ResultCode.FORBIDDEN,"该账号已被禁用");
        }

        //4.BCrypt 校验密码：把用户输入的明文和数据库里的哈希比对
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            log.warn("登录失败：密码错误 -> {}",request.getUsername());
            throw new BusinessException(ResultCode.PASSWORD_ERROR,"用户名或密码错误");
        }

        //5.更新最后登录时间（失败不影响登录，所以只记日志不抛异常）
        try{
            sysUserMapper.updateLastLoginTime(user.getId());
        }catch (Exception e){
            log.warn("更新最后登录时间失败",e);
        }

        //6.签发Token（角色暂时写死 admin,后面做角色表再改成查库）
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), "admin");

        //7.组装返回，不返回password
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtil.getExpireSeconds());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole("admin");

        log.info("登录成功 -> {}",user.getUsername());
        return response;
    }
}
