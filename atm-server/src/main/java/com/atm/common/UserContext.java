package com.atm.common;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 * <p>
 * 拦截器校验通过后把用户信息存进来，Service 层用 getUserId() 就能拿到。
 * <p>
 * ⚠️ 用完必须调用 clear()，否则线程池复用会串号！
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    //存（拦截器调用）
    public static void set(Long userId, String username){
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public static Long getUserId(){
        return USER_ID.get();
    }

    public static String getUsername(){
        return USERNAME.get();
    }

    //清理（afterCompletion）调用
    public static void clear(){
        USER_ID.remove();
        USERNAME.remove();
    }
}
