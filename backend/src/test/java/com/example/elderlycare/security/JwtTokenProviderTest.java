package com.example.elderlycare.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "ElderlyCareSecretKey2026SecureKeyWithAtLeast256BitsLength!",
                3600000L
        );
    }

    @Test
    @DisplayName("测试生成有效 JWT 令牌并能正确提取 Claims")
    void shouldGenerateAndParseTokenSuccessfully() {
        String token = jwtTokenProvider.generateToken(
                100L,
                "admin",
                1L,
                List.of("ORG_ADMIN", "CAREGIVER")
        );

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin", jwtTokenProvider.getUsername(token));
        assertEquals(100L, jwtTokenProvider.getUserId(token));
        assertEquals(1L, jwtTokenProvider.getOrgId(token));
        assertEquals(List.of("ORG_ADMIN", "CAREGIVER"), jwtTokenProvider.getRoles(token));
    }

    @Test
    @DisplayName("测试篡改令牌签名后校验失败")
    void shouldFailWhenTokenIsTampered() {
        String token = jwtTokenProvider.generateToken(100L, "admin", 1L, List.of("ORG_ADMIN"));
        String tamperedToken = token + "xyz";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("测试过期令牌校验失败")
    void shouldFailWhenTokenIsExpired() {
        // 创建已过期的 Provider (过期时间负数)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "ElderlyCareSecretKey2026SecureKeyWithAtLeast256BitsLength!",
                -1000L
        );

        String expiredToken = expiredProvider.generateToken(100L, "admin", 1L, List.of("ORG_ADMIN"));
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
