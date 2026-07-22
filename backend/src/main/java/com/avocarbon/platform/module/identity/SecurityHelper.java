package com.avocarbon.platform.module.identity;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    public User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new BadCredentialsException("User not authenticated");
    }

    public void validateSiteAccess(String siteId) {
        User user = getAuthenticatedUser();
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.DIRECTION) {
            return;
        }
        if (siteId == null || user.getAssignedSites() == null || !user.getAssignedSites().contains(siteId)) {
            throw new AccessDeniedException("Access denied: site '" + siteId + "' is not assigned to user");
        }
    }
    
    public boolean isSiteAuthorized(String siteId) {
        try {
            User user = getAuthenticatedUser();
            if (user.getRole() == Role.ADMIN || user.getRole() == Role.DIRECTION) {
                return true;
            }
            return siteId != null && user.getAssignedSites() != null && user.getAssignedSites().contains(siteId);
        } catch (Exception e) {
            return false;
        }
    }
}
