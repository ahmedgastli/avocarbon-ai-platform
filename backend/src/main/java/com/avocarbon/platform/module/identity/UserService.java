package com.avocarbon.platform.module.identity;

import com.avocarbon.platform.module.project.ProjectResponse;
import com.avocarbon.platform.module.identity.UserRequest;
import com.avocarbon.platform.module.identity.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    List<ProjectResponse> getUserProjects(Long userId);
}