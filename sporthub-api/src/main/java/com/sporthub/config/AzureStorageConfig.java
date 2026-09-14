package com.sporthub.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfig {

    @Bean
    public BlobServiceClient blobServiceClient(
            @Value("${azure.storage.connection-string}")
            String connectionString) {

        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Bean
    public BlobContainerClient blobContainerClient(
            BlobServiceClient blobServiceClient,
            @Value("${azure.storage.container-name}")
            String containerName) {

        BlobContainerClient containerClient =
                blobServiceClient
                        .getBlobContainerClient(containerName);

        containerClient.createIfNotExists();

        return containerClient;
    }
}