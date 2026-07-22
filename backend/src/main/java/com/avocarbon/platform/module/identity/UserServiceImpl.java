package com.avocarbon.platform.module.identity;

import com.avocarbon.platform.module.project.ProjectResponse;
import com.avocarbon.platform.module.identity.UserRequest;
import com.avocarbon.platform.module.identity.UserResponse;
import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.exception.UserAlreadyExistsException;
import com.avocarbon.platform.module.project.ProjectRepository;
import com.avocarbon.platform.module.identity.UserRepository;
import com.avocarbon.platform.module.identity.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserRequest request) {

        log.info("Creating user {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.getEmail()
            );
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .assignedSites(request.getAssignedSites() != null ? request.getAssignedSites() : new java.util.ArrayList<>())
                .build();

        User saved = userRepository.save(user);

        log.info("User created successfully with id {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        log.info("Fetching all users");

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        log.info("Fetching user {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        ));

        return mapToResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {

        log.info("Updating user {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        ));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.getEmail()
            );
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        if (request.getAssignedSites() != null) {
            user.setAssignedSites(request.getAssignedSites());
        }

        User updated = userRepository.save(user);

        log.info("User {} updated successfully", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    public void deleteUser(Long id) {

        log.info("Deleting user {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        ));

        userRepository.delete(user);

        log.info("User {} deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(Long userId) {
        log.info("Fetching projects for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return projectRepository.findByOwnerId(userId)
                .stream()
                .map(this::mapToProjectResponse)
                .toList();
    }

    private UserResponse mapToResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .assignedSites(user.getAssignedSites())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .ownerId(project.getOwner().getId())
                .ownerName(
                        project.getOwner().getFirstName()
                                + " "
                                + project.getOwner().getLastName()
                )
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}