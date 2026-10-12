package com.sporthub.controller;

import com.sporthub.constant.Permissions;
import com.sporthub.dto.request.UpdateRolePermissionsRequest;
import com.sporthub.dto.response.AdminPermissionResponse;
import com.sporthub.dto.response.AdminRoleResponse;
import com.sporthub.service.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sporthub.security.CustomUserDetailsService;
import com.sporthub.security.JwtAuthenticationFilter;
import org.springframework.security.crypto.password.PasswordEncoder;

@WebMvcTest(AdminAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAccessControllerTest {

    @Autowired private MockMvc mockMvc;
    
    @MockitoBean private AccessControlService accessControlService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private PasswordEncoder passwordEncoder;

    @Test
    void get_roles_success_should_return_200() throws Exception {
        AdminRoleResponse response = new AdminRoleResponse(1L, "USER", 5L, List.of(Permissions.CREATE_BOOKING));

        when(accessControlService.getRoles()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/access/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("USER"))
                .andExpect(jsonPath("$[0].userCount").value(5))
                .andExpect(jsonPath("$[0].permissions[0]").value(Permissions.CREATE_BOOKING));

        verify(accessControlService).getRoles();
    }

    @Test
    void get_permissions_success_should_return_200() throws Exception {
        AdminPermissionResponse response = new AdminPermissionResponse(1L, Permissions.VIEW_USERS, "Permission: " + Permissions.VIEW_USERS);

        when(accessControlService.getPermissions()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/access/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value(Permissions.VIEW_USERS));

        verify(accessControlService).getPermissions();
    }

    @Test
    void update_role_permissions_success_should_return_200() throws Exception {
        AdminRoleResponse response = new AdminRoleResponse(2L, "OWNER", 3L, List.of(Permissions.CREATE_BOOKING, Permissions.VIEW_OWN_BOOKINGS));

        when(accessControlService.updateRolePermissions(eq(2L), any(UpdateRolePermissionsRequest.class))).thenReturn(response);

        String json = "{\"permissionCodes\":[\"CREATE_BOOKING\",\"VIEW_OWN_BOOKINGS\"]}";

        mockMvc.perform(put("/api/admin/access/roles/2/permissions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("OWNER"))
                .andExpect(jsonPath("$.userCount").value(3))
                .andExpect(jsonPath("$.permissions[0]").value(Permissions.CREATE_BOOKING))
                .andExpect(jsonPath("$.permissions[1]").value(Permissions.VIEW_OWN_BOOKINGS));

        verify(accessControlService).updateRolePermissions(eq(2L), any(UpdateRolePermissionsRequest.class));
    }

    @Test
    void update_role_permissions_null_list_should_return_400() throws Exception {
        String json = "{\"permissionCodes\":null}";

        mockMvc.perform(put("/api/admin/access/roles/2/permissions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verify(accessControlService, never()).updateRolePermissions(anyLong(), any(UpdateRolePermissionsRequest.class));
    }

    @Test
    void update_role_permissions_blank_code_should_return_400() throws Exception {
        String json = "{\"permissionCodes\":[\"\"]}";

        mockMvc.perform(put("/api/admin/access/roles/2/permissions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verify(accessControlService, never()).updateRolePermissions(anyLong(), any(UpdateRolePermissionsRequest.class));
    }

    @Test
    void update_role_permissions_invalid_role_id_should_return_400() throws Exception {
        String json = "{\"permissionCodes\":[\"CREATE_BOOKING\"]}";

        mockMvc.perform(put("/api/admin/access/roles/0/permissions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verify(accessControlService, never()).updateRolePermissions(anyLong(), any(UpdateRolePermissionsRequest.class));
    }
}