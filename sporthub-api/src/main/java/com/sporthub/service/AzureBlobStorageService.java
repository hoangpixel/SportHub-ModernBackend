package com.sporthub.service;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AzureBlobStorageService {

    private final BlobContainerClient blobContainerClient;

    private static final long MAX_FILE_SIZE =
            5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    public String uploadImage(
            MultipartFile file,
            String folder) {

        validateImage(file);

        String extension =
                getExtension(
                        file.getOriginalFilename()
                );

        String blobName =
                folder
                + "/"
                + UUID.randomUUID()
                + extension;

        BlobClient blobClient =
                blobContainerClient
                        .getBlobClient(blobName);

        try {

            blobClient.upload(
                    file.getInputStream(),
                    file.getSize(),
                    true
            );

            blobClient.setHttpHeaders(
                    new BlobHttpHeaders()
                            .setContentType(
                                    file.getContentType()
                            )
            );

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Không thể upload hình ảnh"
            );
        }

        return blobClient.getBlobUrl();
    }

    private void validateImage(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Hình ảnh không được để trống"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "Hình ảnh không được vượt quá 5MB"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType)) {

            throw new IllegalArgumentException(
                    "Chỉ hỗ trợ JPG, PNG hoặc WEBP"
            );
        }
    }

    private String getExtension(
            String filename) {

        if (filename == null
                || !filename.contains(".")) {

            return "";
        }

        return filename.substring(
                filename.lastIndexOf('.')
        ).toLowerCase();
    }
}