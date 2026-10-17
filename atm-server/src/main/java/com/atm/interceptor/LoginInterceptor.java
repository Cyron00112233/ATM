package com.atm.interceptor;

import com.atm.common.UserContext;
import com.atm.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor{

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,
                             Object handle)throws Exception {
        //// 放行浏览器的跨域预检请求（OPTIONS），否则前端跨域调不通
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        //1.请求体头
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return reject(response, "未登录，请先登录");
        }

        //2.去掉“Bearer”前缀，拿到纯Token
        String token = header.substring(PREFIX.length()).trim();
        if (token.isEmpty()) {
            return reject(response, "未登录，请先登录");
        }

        //验签+检查过期（失败会抛异常，统一捕获）
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);

            //4.存进THreadLocal,Service层就能拿到当前用户
            UserContext.set(userId, username);
            return true;      //放行
        } catch (Exception e) {
            log.warn("Token 校验失败 -> {}", e.getMessage());
            return reject(response, "登录已过期，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,HttpServletResponse response,
                                Object handler,Exception ex){
        //必须清理，线程池复用线程，不清理会窜号
        UserContext.clear();
    }


    /**
     * 统一的 401 响应
     * <p>
     * ⚠️ 拦截器里不能用全局异常处理器（还没进入 Controller 处理链），
     * 所以要手动往响应流里写 JSON。
     */
    private boolean reject(HttpServletResponse response,String message) throws Exception{
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   //HTTP 401
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}"
        );
        return false;                    //false = 拦截
    }

}
