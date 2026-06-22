package com.interview.util;

/**
 * 用户上下文 - 使用ThreadLocal存储当前请求的用户信息
 */
public class UserContext {

    private static final ThreadLocal<Long> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> username = new ThreadLocal<>();
    private static final ThreadLocal<Integer> roleType = new ThreadLocal<>();

    public static void setUserId(Long id) {
        userId.set(id);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void setUsername(String name) {
        username.set(name);
    }

    public static String getUsername() {
        return username.get();
    }

    public static void setRoleType(Integer type) {
        roleType.set(type);
    }

    public static Integer getRoleType() {
        return roleType.get();
    }

    /**
     * 清理上下文（请求结束后必须调用，防止内存泄漏）
     */
    public static void clear() {
        userId.remove();
        username.remove();
        roleType.remove();
    }
}
