package com.example.wms.identity.infrastructure.security;

import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.Role;
import com.example.wms.shared.domain.ForbiddenOperationException;
import java.util.Arrays;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityAccessControl implements AccessControl {

    @Override
    public void requireAny(Role... roles) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var allowed = authentication != null && authentication.isAuthenticated()
                && Arrays.stream(roles).anyMatch(role -> authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name())));
        if (!allowed) {
            throw new ForbiddenOperationException("security.forbidden", "You are not allowed to perform this operation");
        }
    }
}
