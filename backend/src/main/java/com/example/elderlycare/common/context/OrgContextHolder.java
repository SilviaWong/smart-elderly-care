package com.example.elderlycare.common.context;

/**
 * 机构数据隔离与当前用户上下文持有器（基于 ThreadLocal）
 */
public final class OrgContextHolder {

    private static final ThreadLocal<Long> CURRENT_ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();

    private OrgContextHolder() {
    }

    public static void setOrgId(Long orgId) {
        CURRENT_ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return CURRENT_ORG_ID.get();
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setUsername(String username) {
        CURRENT_USERNAME.set(username);
    }

    public static String getUsername() {
        return CURRENT_USERNAME.get();
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
        CURRENT_USER_ID.remove();
        CURRENT_USERNAME.remove();
    }
}
