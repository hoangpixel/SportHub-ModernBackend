package com.sporthub.controller;

import com.sporthub.dto.request.SportRequest;
import com.sporthub.dto.response.SportResponse;
import com.sporthub.service.SportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sports")
@RequiredArgsConstructor
@Validated
public class AdminSportController {

    private final SportService sportService;

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).CREATE_SPORT)")
    public ResponseEntity<SportResponse> createSport(@Valid @RequestBody SportRequest request) {
        SportResponse response = sportService.createSport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_SPORT)")
    public ResponseEntity<SportResponse> updateSport(@PathVariable @Positive(message = "ID phải lớn hơn 0") Long id, @Valid @RequestBody SportRequest request) {
        SportResponse response = sportService.updateSport(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_SPORT)")
    public ResponseEntity<SportResponse> updateIcon(@PathVariable @Positive(message = "ID phải lớn hơn 0") Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(sportService.updateIcon(id, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).DELETE_SPORT)")
    public ResponseEntity<Map<String, String>> deleteSport(@PathVariable @Positive(message = "ID phải lớn hơn 0") Long id) {
        sportService.deleteSport(id);
        return ResponseEntity.ok(Map.of("message", "Xóa môn thể thao thành công"));
    }
}