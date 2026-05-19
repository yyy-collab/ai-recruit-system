package com.recruit.airecruitsystem.utils;

public class UserContext {
    // ThreadLocal 确保每个线程（每个请求）拥有独立的用户id和角色副本，避免并发冲突
    private static final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentRole = new ThreadLocal<>();

    public static void setUserId(Integer id) { currentUserId.set(id); }
    public static Integer getUserId() { return currentUserId.get(); }
    public static void setRole(String role) { currentRole.set(role); }
    public static String getRole() { return currentRole.get(); }

    // 请求结束后清理，防止内存泄漏
    public static void clear() {
        currentUserId.remove();
        currentRole.remove();
    }
}