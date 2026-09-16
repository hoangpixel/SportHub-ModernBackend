package com.sporthub.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.dto.request.SportRequest;
import com.sporthub.dto.response.SportResponse;
import com.sporthub.entity.Sport;
import com.sporthub.enums.SportStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.SportRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class SportServiceTest {
    @Mock
    private SportRepository sportRepository;

    @Mock
    private AzureBlobStorageService azureBlobStorageService;

    private SportService sportService;

    @BeforeEach
    void setUp() {
        sportService = new SportService(sportRepository, azureBlobStorageService);
    }

    private Sport createSport() {

        Sport sport = new Sport();

        sport.setId(1L);
        sport.setName("Football");
        sport.setDescription("Bóng đá");
        sport.setIconUrl("old-icon");
        sport.setStatus(SportStatus.ACTIVE);

        return sport;
    }

    private SportRequest createSportRequest() {

        SportRequest request =
                new SportRequest();

        request.setName("Football");
        request.setDescription("Bóng đá");
        request.setIconUrl("icon-url");

        return request;
    }

    // Tìm thấy sport
    @Test 
    void get_sport_by_id_success() {
        Long id = 1L;
        Sport sport = createSport();

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));
        SportResponse reponse = sportService.getSportById(id);

        assertNotNull(reponse);
        assertEquals(sport.getId(), reponse.getId());
        assertEquals(sport.getName(), reponse.getName());

        verify(sportRepository, times(1)).findById(id);
    }

    // Kh tìm thấy sport
    @Test 
    void get_sport_by_id_not_found() {
        Long id = 1L;
        when(sportRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> sportService.getSportById(id));

        assertEquals("Không tìm thấy môn thể thao có id = " + id, exception.getMessage());
    }

    // Thêm sport thành công
    @Test 
    void create_sport_success() {
        SportRequest request = createSportRequest();

        when(sportRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);

        SportResponse response = sportService.createSport(request);

        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getIconUrl(), response.getIconUrl());

        verify(sportRepository).save(any(Sport.class));
    }

    // Thêm sport thất bại
    @Test 
    void create_sport_duplicate_name_shouldThrowConflict() {
        SportRequest request = createSportRequest();
        when(sportRepository.existsByNameIgnoreCase("Football")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> sportService.createSport(request));

        assertEquals("Môn thể thao đã tồn tại", exception.getMessage());

        verify(sportRepository, never()).save(any(Sport.class));
    }

    // Cập nhật sport thành công do không trùng tên sport
    @Test 
    void update_sport_success() {
        SportRequest request = createSportRequest();
        request.setName("Tennis");
        Sport sport = createSport();
        Long id = 1L;

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));

        when(sportRepository.findByNameIgnoreCase(request.getName().trim())).thenReturn(Optional.empty());
        when(sportRepository.saveAndFlush(any(Sport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SportResponse response = sportService.updateSport(id, request);

        assertNotNull(response);

        assertEquals(id, response.getId());
        assertEquals(request.getName().trim(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getIconUrl(), response.getIconUrl());

        verify(sportRepository, times(1)).saveAndFlush(any(Sport.class));
    }

    // Cập nhật sport thất bại do trùng tên sport
    @Test 
    void update_sport_duplicate_name_shouldThrowConflict() {
        Long id = 1L;
        SportRequest request = createSportRequest();
        request.setName("Tennis");

        Sport sport = createSport();

        Sport existingSport = createSport();
        existingSport.setId(2L); 
        existingSport.setName("Tennis");

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));

        when(sportRepository.findByNameIgnoreCase(request.getName().trim())).thenReturn(Optional.of(existingSport));

        ConflictException exception = assertThrows(ConflictException.class, () -> sportService.updateSport(id, request));

        assertEquals("Tên môn thể thao đã tồn tại", exception.getMessage());

        verify(sportRepository, never()).saveAndFlush(any(Sport.class));
    }

    // Cập nhật thành công với cùng 1 id
    @Test 
    void update_sport_same_name_same_id_success() {
        SportRequest request = createSportRequest();
        request.setName("Tennis");

        Long id = 1L;
        Sport sport = createSport();

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));
        when(sportRepository.findByNameIgnoreCase(request.getName().trim())).thenReturn(Optional.of(sport));
        when(sportRepository.saveAndFlush(any(Sport.class))).thenAnswer(innovacation -> innovacation.getArgument(0));

        SportResponse response = sportService.updateSport(id, request);

        assertNotNull(response);
        
        assertEquals(sport.getId(), response.getId());
        assertEquals(sport.getName(), response.getName());
        assertEquals(sport.getDescription(), response.getDescription());
        assertEquals(sport.getIconUrl(), response.getIconUrl());

        verify(sportRepository, times(1)).saveAndFlush(any(Sport.class));
    }

    // Update thất bại do không tìm thấy Sport
    @Test
    void update_sport_not_found() {
        SportRequest request = createSportRequest();
        Long id = 1L;
        when(sportRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> sportService.updateSport(id, request));

        assertEquals("Không tìm thấy môn thể thao có id = " + id, exception.getMessage());
        verify(sportRepository, never()).saveAndFlush(any(Sport.class));
    }

    // Xóa thành công
    @Test 
    void delete_sport_success() {
        Long id = 1L;
        Sport sport = createSport();

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));

        sportService.deleteSport(id);
        
        verify(sportRepository).delete(sport);
    }

    // Xóa thất bại do không tìm thấy id
    @Test 
    void delete_sport_not_found() {
        when(sportRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> sportService.deleteSport(1L));

        assertEquals("Không tìm thấy môn thể thao có id = " + 1L, exception.getMessage());

        verify(sportRepository, never()).delete(any(Sport.class));
    }

    // Cập nhật thành công Icon
    @Test 
    void update_icon_success() {
        Long id = 1L;
        Sport sport = createSport();

        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());
        String imageIcon = "https://fake-storage.blob.core.windows.net/" + "sporthub-images/sports/image.jpg";

        when(sportRepository.findById(id)).thenReturn(Optional.of(sport));
        when(azureBlobStorageService.uploadImage(file, "sports")).thenReturn(imageIcon);
        when(sportRepository.saveAndFlush(any(Sport.class))).thenAnswer(inno -> inno.getArgument(0));

        SportResponse response = sportService.updateIcon(id, file);

        assertNotNull(response);

        assertEquals(imageIcon, response.getIconUrl());

        verify(azureBlobStorageService).uploadImage(file, "sports");
        verify(sportRepository).saveAndFlush(sport);
    }

    // Cập nhật thất bại Icon vì kh tìm thấy Id
    @Test 
    void update_icon_sport_not_found() {
        Long id = 1L;
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());

        when(sportRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> sportService.updateIcon(id, file));

        assertEquals("Không tìm thấy môn thể thao có id = " + id, exception.getMessage());

        verify(azureBlobStorageService, never()).uploadImage(any(), anyString());
        verify(sportRepository, never()).saveAndFlush(any(Sport.class));
    }

    // Lấy toàn bộ danh sách mà không có keyword
    @Test 
    void get_all_sports_without_keyword() {
        Sport sport1 = createSport();
        Sport sport2 = createSport();

        sport2.setId(2L);
        sport2.setName("Tennis");

        Page<Sport> sportPage = new PageImpl<>(List.of(sport1, sport2));

        when(sportRepository.findAll(any(Pageable.class))).thenReturn(sportPage);

        Page<SportResponse> response = sportService.getAllSports(null, 0, 10);

        assertNotNull(response);

        assertEquals(2, response.getContent().size());
    }

    // Lấy toàn danh sách có keyword
    @Test
    void get_all_sports_with_keyword() {
        Sport sport1 = createSport();
        Sport sport2 = createSport();

        sport2.setId(2L);
        sport2.setName("Tennis");

        String keyword = "Football";

        Page<Sport> sportPage = new PageImpl<>(List.of(sport1));

        when(sportRepository.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class))).thenReturn(sportPage);

        Page<SportResponse> response = sportService.getAllSports(keyword, 0, 10);

        assertNotNull(response);

        assertEquals(1, response.getContent().size());
    }
}
