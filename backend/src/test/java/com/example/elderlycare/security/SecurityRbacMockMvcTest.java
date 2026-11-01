package com.example.elderlycare.security;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.system.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRbacMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysUserService sysUserService;

    @Test
    @DisplayName("未携带 Token 访问受保护的系统用户 API 返回 401")
    void shouldReturn401WhenAccessingProtectedApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/system/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(username = "visitor", roles = {"GUEST"})
    @DisplayName("无对应权限的角色访问受保护接口返回 403")
    void shouldReturn403WhenUserLacksRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/system/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"system:user:view"})
    @DisplayName("拥有相应权限的用户访问受保护接口正常放行")
    void shouldAllowWhenUserHasRequiredAuthority() throws Exception {
        when(sysUserService.pageUsers(any())).thenReturn(
                PageResult.of(Collections.emptyList(), 0, 1, 10)
        );

        mockMvc.perform(get("/api/v1/system/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
