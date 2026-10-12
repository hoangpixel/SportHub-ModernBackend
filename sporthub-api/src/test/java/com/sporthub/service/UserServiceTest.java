package com.sporthub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import com.sporthub.dto.request.ChangePasswordRequest;
import com.sporthub.dto.request.UpdateUserStatusRequest;
import com.sporthub.dto.response.UserResponse;
import com.sporthub.entity.User;
import com.sporthub.entity.UserProfile;
import com.sporthub.enums.UserStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.PermissionRepository;
import com.sporthub.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.sporthub.dto.request.CreateAdminUserRequest;
import com.sporthub.dto.request.UpdateAdminUserRequest;
import com.sporthub.entity.Role;
import com.sporthub.entity.UserRole;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.RoleRepository;
import com.sporthub.repository.UserRoleRepository;
import java.util.Set;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock 
    private UserRepository userRepository;

    @Mock 
    private PasswordEncoder passwordEncoder;

    @Mock 
    private AzureBlobStorageService azureBlobStorageService;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, userRoleRepository, passwordEncoder, permissionRepository, azureBlobStorageService);
    }

    private Role createRole() {
    Role role = new Role();
    role.setId(1L);
    role.setName("USER");
    return role;
}   

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user01");
        user.setEmail("user01@gmail.com");
        user.setPasswordHash("hased-password");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private ChangePasswordRequest createChangePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("87654321");
        request.setNewPassword("12345678");
        request.setConfirmPassword("12345678");
        return  request;
    }

    // Update status thành công
    @Test 
    void update_status_success() {
        Long id = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        User user = createUser();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(permissionRepository.findPermissionsByUsername("user01")).thenReturn(List.of());

        UserResponse response = userService.updateStatus(id, request);

        assertEquals(UserStatus.INACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    // Update status thất bại
    @Test
    void update_status_user_not_found_shouldThrowException() {
        Long id = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.updateStatus(id, request));

        assertEquals("Không tìm thấy user có id = " + id, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // Update password thành công
    @Test 
    void update_password_success() {

        ChangePasswordRequest request = createChangePasswordRequest();

        User user = createUser();
        
        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(true);

        when(passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())).thenReturn(false);

        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("new-hashed-password");

        userService.changePassword("user01", request);

        assertEquals("new-hashed-password", user.getPasswordHash());

        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(user);
    }

    // Update password mà sai oldPassword
    @Test 
    void update_password_but_wrong_oldPassword() {
        ChangePasswordRequest request = createChangePasswordRequest();
        User user = createUser();

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user01", request));

        assertEquals("Mật khẩu cũ không chính xác", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Update password mà newPassword với confirmPassword không trùng
    @Test 
    void update_password_but_newPassword_and_confirmPassword_no_duplicate() {
        ChangePasswordRequest request = createChangePasswordRequest();
        request.setConfirmPassword("11111111");

        User user = createUser();

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user01", request));

        assertEquals("Xác nhận mật khẩu không khớp", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Update password mà newPassword trùng với user password
    @Test 
    void update_password_but_newPassword_and_userPassword_duplicate() {
        ChangePasswordRequest request = createChangePasswordRequest();
        User user = createUser();

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user01", request));

        assertEquals("Mật khẩu mới không được trùng mật khẩu cũ", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Update Avatar thành công
    @Test 
    void update_avatar_existing_profile_success() {
        User user = createUser();
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        user.setProfile(userProfile);

        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());

        String imageUrl = "https://fake-storage.blob.core.windows.net/" + "sporthub-images/avatars/avatar.jpg";

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(azureBlobStorageService.uploadImage(file, "avatars")).thenReturn(imageUrl);

        UserResponse reponse = userService.updateAvatar("user01", file);

        assertEquals(imageUrl, user.getProfile().getAvatarUrl());

        verify(azureBlobStorageService).uploadImage(file, "avatars");
        verify(userRepository).save(user);
    }

    // Update Avatar mà chưa có tồn tại UserProfile
    @Test 
    void update_avatar_without_profile_should_create_profile() {
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());
        
        User user = createUser();
        user.setProfile(null);

        String imageUrl = "https://fake-storage.blob.core.windows.net/" + "sporthub-images/avatars/avatar.jpg";

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(azureBlobStorageService.uploadImage(file, "avatars")).thenReturn(imageUrl);

        UserResponse response = userService.updateAvatar("user01", file);

        assertNotNull(user.getProfile());
        assertEquals(user, user.getProfile().getUser());
        assertEquals(imageUrl, user.getProfile().getAvatarUrl());
        
        verify(azureBlobStorageService).uploadImage(file, "avatars");
        verify(userRepository).save(user);
    }

    // Update Avatar thất bại do usernamae hong tồn tại
    @Test 
    void update_avatar_not_found_shouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());
        when(userRepository.findByUsername("user01")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.updateAvatar("user01", file));

        assertEquals("Không tìm thấy người dùng", exception.getMessage());

        verify(azureBlobStorageService, never()).uploadImage(any(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Tạo user thành công
@Test
void create_user_success() {
    CreateAdminUserRequest request = new CreateAdminUserRequest();
    request.setUsername("newuser");
    request.setEmail("newuser@gmail.com");
    request.setPassword("12345678");
    request.setFullName("New User");
    request.setStatus(UserStatus.ACTIVE);
    request.setRoles(Set.of("USER"));

    Role role = createRole();

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
    when(passwordEncoder.encode("12345678")).thenReturn("hashed-password");

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User savedUser = invocation.getArgument(0);
        savedUser.setId(1L);
        return savedUser;
    });

    User createdUser = new User();
    createdUser.setId(1L);
    createdUser.setUsername("newuser");
    createdUser.setEmail("newuser@gmail.com");
    createdUser.setPasswordHash("hashed-password");
    createdUser.setStatus(UserStatus.ACTIVE);

    UserProfile profile = new UserProfile();
    profile.setUser(createdUser);
    profile.setFullName("New User");
    createdUser.setProfile(profile);

    UserRole userRole = new UserRole();
    userRole.setUser(createdUser);
    userRole.setRole(role);
    createdUser.getUserRoles().add(userRole);

    when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(createdUser));
    when(permissionRepository.findPermissionsByUsername("newuser")).thenReturn(List.of());

    UserResponse response = userService.createUser(request);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("newuser", response.getUsername());
    assertEquals("newuser@gmail.com", response.getEmail());
    assertEquals("New User", response.getFullName());
    assertTrue(response.getRoles().contains("USER"));

    verify(passwordEncoder).encode("12345678");
    verify(userRepository).save(any(User.class));
    verify(userRoleRepository).save(any(UserRole.class));
}

// Tạo user thất bại do username đã tồn tại
@Test
void create_user_duplicate_username_shouldThrowConflict() {
    CreateAdminUserRequest request = new CreateAdminUserRequest();
    request.setUsername("user01");
    request.setEmail("new@gmail.com");
    request.setPassword("12345678");
    request.setRoles(Set.of("USER"));

    when(userRepository.existsByUsername("user01")).thenReturn(true);

    ConflictException exception = assertThrows(ConflictException.class, () -> userService.createUser(request));

    assertEquals("Username đã tồn tại: user01", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
    verify(userRoleRepository, never()).save(any(UserRole.class));
}

// Tạo user thất bại do email đã tồn tại
@Test
void create_user_duplicate_email_shouldThrowConflict() {
    CreateAdminUserRequest request = new CreateAdminUserRequest();
    request.setUsername("newuser");
    request.setEmail("user01@gmail.com");
    request.setPassword("12345678");
    request.setRoles(Set.of("USER"));

    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("user01@gmail.com")).thenReturn(true);

    ConflictException exception = assertThrows(ConflictException.class, () -> userService.createUser(request));

    assertEquals("Email đã tồn tại: user01@gmail.com", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
}

// Cập nhật user thành công
@Test
void update_user_success() {
    Long id = 1L;

    User user = createUser();

    UpdateAdminUserRequest request = new UpdateAdminUserRequest();
    request.setUsername("user02");
    request.setEmail("user02@gmail.com");
    request.setFullName("User Updated");
    request.setPhone("0901234567");
    request.setCity("Ho Chi Minh");

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("user02")).thenReturn(false);
    when(userRepository.existsByEmail("user02@gmail.com")).thenReturn(false);
    when(permissionRepository.findPermissionsByUsername("user02")).thenReturn(List.of());

    UserResponse response = userService.updateUser(id, request);

    assertNotNull(response);
    assertEquals("user02", user.getUsername());
    assertEquals("user02@gmail.com", user.getEmail());
    assertEquals("User Updated", user.getProfile().getFullName());
    assertEquals("0901234567", user.getProfile().getPhone());
    assertEquals("Ho Chi Minh", user.getProfile().getCity());

    verify(userRepository).save(user);
}

// Cập nhật user thất bại do username đã tồn tại
@Test
void update_user_duplicate_username_shouldThrowConflict() {
    Long id = 1L;

    User user = createUser();

    UpdateAdminUserRequest request = new UpdateAdminUserRequest();
    request.setUsername("existinguser");

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("existinguser")).thenReturn(true);

    ConflictException exception = assertThrows(ConflictException.class, () -> userService.updateUser(id, request));

    assertEquals("Username đã tồn tại: existinguser", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
}

// Cập nhật user thất bại do email đã tồn tại
@Test
void update_user_duplicate_email_shouldThrowConflict() {
    Long id = 1L;

    User user = createUser();

    UpdateAdminUserRequest request = new UpdateAdminUserRequest();
    request.setEmail("existing@gmail.com");

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

    ConflictException exception = assertThrows(ConflictException.class, () -> userService.updateUser(id, request));

    assertEquals("Email đã tồn tại: existing@gmail.com", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
}

// Cập nhật user thất bại do không tìm thấy user
@Test
void update_user_not_found_shouldThrowException() {
    Long id = 999L;

    UpdateAdminUserRequest request = new UpdateAdminUserRequest();
    request.setFullName("Test");

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(id, request));

    assertEquals("Không tìm thấy user có id = " + id, exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
}

// Vô hiệu hóa user thành công
@Test
void deactivate_user_success() {
    Long id = 1L;

    User user = createUser();

    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    userService.deactivateUser(id);

    assertEquals(UserStatus.INACTIVE, user.getStatus());
    verify(userRepository).save(user);
}

// Vô hiệu hóa user thất bại do không tìm thấy user
@Test
void deactivate_user_not_found_shouldThrowException() {
    Long id = 999L;

    when(userRepository.findById(id)).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.deactivateUser(id));

    assertEquals("Không tìm thấy user có id = " + id, exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
}

// Admin cập nhật avatar user thành công
@Test
void update_user_avatar_success() {
    Long id = 1L;

    User user = createUser();

    MultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "image".getBytes());

    String imageUrl = "https://fake-storage.blob.core.windows.net/sporthub-images/avatars/avatar.jpg";

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.of(user));
    when(azureBlobStorageService.uploadImage(file, "avatars")).thenReturn(imageUrl);
    when(permissionRepository.findPermissionsByUsername("user01")).thenReturn(List.of());

    UserResponse response = userService.updateUserAvatar(id, file);

    assertNotNull(response);
    assertNotNull(user.getProfile());
    assertEquals(imageUrl, user.getProfile().getAvatarUrl());

    verify(azureBlobStorageService).uploadImage(file, "avatars");
    verify(userRepository).save(user);
}

// Admin cập nhật avatar thất bại do không tìm thấy user
@Test
void update_user_avatar_not_found_shouldThrowException() {
    Long id = 999L;

    MultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "image".getBytes());

    when(userRepository.findWithRolesById(id)).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.updateUserAvatar(id, file));

    assertEquals("Không tìm thấy user có id = " + id, exception.getMessage());
    verify(azureBlobStorageService, never()).uploadImage(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
}


}
