package com.sporthub.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureBlobStorageServiceTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    private AzureBlobStorageService azureBlobStorageService;

    @BeforeEach
    void setUp() {
        azureBlobStorageService = new AzureBlobStorageService(blobContainerClient);
    }

    // Upload ảnh thành công
    @Test
    void upload_image_success() {
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image".getBytes());

        String imageUrl = "https://fake-storage.blob.core.windows.net/sporthub-images/avatars/image.jpg";

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(imageUrl);

        String response = azureBlobStorageService.uploadImage(file, "avatars");

        assertNotNull(response);
        assertEquals(imageUrl, response);

        ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(blobContainerClient).getBlobClient(blobNameCaptor.capture());

        String blobName = blobNameCaptor.getValue();

        assertTrue(blobName.startsWith("avatars/"));
        assertTrue(blobName.endsWith(".jpg"));

        verify(blobClient).upload(any(InputStream.class), eq(file.getSize()), eq(true));
        verify(blobClient).setHttpHeaders(any());
    }

    // Upload file rỗng
    @Test
    void upload_image_empty_file_shouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> azureBlobStorageService.uploadImage(file, "avatars"));

        assertEquals("Hình ảnh không được để trống", exception.getMessage());

        verify(blobContainerClient, never()).getBlobClient(anyString());
    }

    // Upload file vượt quá 5MB
    @Test
    void upload_image_too_large_shouldThrowException() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5 * 1024 * 1024L + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> azureBlobStorageService.uploadImage(file, "avatars"));

        assertEquals("Hình ảnh không được vượt quá 5MB", exception.getMessage());

        verify(blobContainerClient, never()).getBlobClient(anyString());
    }

    // Upload file sai định dạng
    @Test
    void upload_image_invalid_content_type_shouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", "document.txt", "text/plain", "hello".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> azureBlobStorageService.uploadImage(file, "avatars"));

        assertEquals("Chỉ hỗ trợ JPG, PNG hoặc WEBP", exception.getMessage());

        verify(blobContainerClient, never()).getBlobClient(anyString());
    }

    // Upload bị IOException
    @Test
    void upload_image_io_exception_shouldThrowException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("image.jpg");
        when(file.getInputStream()).thenThrow(new IOException("Upload error"));

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> azureBlobStorageService.uploadImage(file, "avatars"));

        assertEquals("Không thể upload hình ảnh", exception.getMessage());

        verify(blobContainerClient).getBlobClient(anyString());
        verify(blobClient, never()).setHttpHeaders(any());
    }
}