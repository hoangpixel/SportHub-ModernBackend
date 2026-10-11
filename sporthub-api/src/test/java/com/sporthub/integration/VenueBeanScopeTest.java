package com.sporthub.integration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.sporthub.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
class VenueBeanScopeTest {

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private BlobServiceClient blobServiceClient;

    @MockitoBean
    private BlobContainerClient blobContainerClient;

    @Test
    void venueServiceIsSameBeanWithinOneContext() {
        VenueService firstBean = context.getBean(VenueService.class);
        VenueService secondBean = context.getBean(VenueService.class);

        assertSame(firstBean, secondBean);

        System.out.println(
                "Singleton check: both lookups return the same VenueService bean"
        );
    }
}