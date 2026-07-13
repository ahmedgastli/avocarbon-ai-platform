package com.avocarbon.platform.dto;

import com.avocarbon.platform.entity.Role;
import lombok.*;

import java.time.Instant;

/**
 * Response DTO returned to clients.
 *
 * Password is never exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private Role role;

    private Boolean enabled;

    private Instant createdAt;

    private Instant updatedAt;

}