package com.example.elderlycare.common.util;

import org.springframework.util.StringUtils;

/**
 * 敏感数据脱敏工具类（身份证、手机号、真实姓名）
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /**
     * 手机号脱敏：保留前3后4，中间4位为* (例如：138****1234)
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 身份证脱敏：保留前6后4，中间8位为* (例如：110101********1234)
     */
    public static String maskIdNo(String idNo) {
        if (!StringUtils.hasText(idNo) || idNo.length() < 10) {
            return idNo;
        }
        if (idNo.length() == 18) {
            return idNo.replaceAll("(\\d{6})\\d{8}(\\w{4})", "$1********$2");
        }
        return idNo.substring(0, 3) + "******" + idNo.substring(idNo.length() - 3);
    }

    /**
     * 姓名脱敏：张* 或 李*三
     */
    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return name;
        }
        if (name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
