package com.avocarbon.platform.module.identity;

import com.avocarbon.platform.module.identity.Role;
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

    private java.util.List<String> assignedSites;

    private Instant createdAt;

    private Instant updatedAt;

}