package com.sporthub.service;

import com.sporthub.dto.request.ChangePasswordRequest;
import com.sporthub.dto.request.CreateAdminUserRequest;
import com.sporthub.dto.request.UpdateAdminUserRequest;
import com.sporthub.dto.request.UpdateProfileRequest;
import com.sporthub.dto.request.UpdateUserRolesRequest;
import com.sporthub.dto.request.UpdateUserStatusRequest;
import com.sporthub.dto.response.UserResponse;
import com.sporthub.entity.Role;
import com.sporthub.entity.User;
import com.sporthub.entity.UserProfile;
import com.sporthub.entity.UserRole;
import com.sporthub.entity.UserRoleId;
import com.sporthub.enums.UserStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.PermissionRepository;
import com.sporthub.repository.RoleRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;
    private final AzureBlobStorageService azureBlobStorageService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users;

        if (keyword == null || keyword.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);
        }

        return users.map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        user.setStatus(request.getStatus());
        userRepository.save(user);

        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .toList();

        List<String> permissions = permissionRepository.findPermissionsByUsername(user.getUsername()).stream()
                .map(permission -> permission.getCode())
                .toList();

        UserProfile profile = user.getProfile();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getGender() : null,
                profile != null ? profile.getDateOfBirth() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getCity() : null,
                user.getStatus(),
                roles,
                permissions
        );
    }

    @Transactional
    public UserResponse updateRoles(Long id, UpdateUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        // Tìm tất cả role trước
        List<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleName)))
                .toList();

        // Chỉ xóa role cũ SAU KHI đã kiểm tra toàn bộ role mới đều hợp lệ.
        userRoleRepository.deleteByUser_Id(id);

        // Tạo lại UserRole
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setId(new UserRoleId(user.getId(), role.getId()));
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        User updatedUser = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        return toUserResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        UserProfile profile = user.getProfile();

        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            profile.setCity(request.getCity());
        }

        userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        String imageUrl = azureBlobStorageService.uploadImage(file, "avatars");

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        profile.setAvatarUrl(imageUrl);
        userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateAdminUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username đã tồn tại: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại: " + request.getEmail());
        }

        List<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleName)))
                .toList();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());

        user.setProfile(profile);
        user = userRepository.save(user);

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setId(new UserRoleId(user.getId(), role.getId()));
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        User createdUser = userRepository.findWithRolesById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user vừa tạo"));

        return toUserResponse(createdUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateAdminUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ConflictException("Username đã tồn tại: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());

        userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateUserAvatar(Long id, MultipartFile file) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user có id = " + id));

        String imageUrl = azureBlobStorageService.uploadImage(file, "avatars");

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        profile.setAvatarUrl(imageUrl);
        userRepository.save(user);

        return toUserResponse(user);
    }
}