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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                null,
                null,
                passwordEncoder,
                permissionRepository,
                azureBlobStorageService
        );
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
}
