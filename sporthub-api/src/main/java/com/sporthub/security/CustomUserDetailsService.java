package com.sporthub.security;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.entity.Permission;
import com.sporthub.entity.User;
import com.sporthub.entity.UserRole;
import com.sporthub.enums.UserStatus;
import com.sporthub.repository.PermissionRepository;
import com.sporthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findWithRolesByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        boolean isAdmin = false;

        for (UserRole userRole : user.getUserRoles()) {
            String roleName = userRole.getRole().getName();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

            if (Roles.ADMIN.equals(roleName)) {
                isAdmin = true;
            }
        }

        if (isAdmin) {
            for (String permissionCode : Permissions.ALL) {
                authorities.add(new SimpleGrantedAuthority(permissionCode));
            }
        } else {
            for (Permission permission : permissionRepository.findPermissionsByUsername(username)) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername()).password(user.getPasswordHash()).authorities(authorities).disabled(user.getStatus() != UserStatus.ACTIVE).build();
    }
}