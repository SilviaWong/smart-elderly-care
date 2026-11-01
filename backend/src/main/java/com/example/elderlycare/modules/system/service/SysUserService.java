package com.example.elderlycare.modules.system.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.system.dto.LoginRequest;
import com.example.elderlycare.modules.system.dto.UserCreateDTO;
import com.example.elderlycare.modules.system.dto.UserQueryDTO;
import com.example.elderlycare.modules.system.dto.UserUpdateDTO;
import com.example.elderlycare.modules.system.vo.LoginResponse;
import com.example.elderlycare.modules.system.vo.UserVO;

public interface SysUserService {

    LoginResponse login(LoginRequest request);

    UserVO getCurrentUser();

    PageResult<UserVO> pageUsers(UserQueryDTO query);

    Long createUser(UserCreateDTO dto);

    void updateUser(Long id, UserUpdateDTO dto);

    void changeUserStatus(Long id, String status);
}
