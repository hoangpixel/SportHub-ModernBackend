package com.sporthub.dto.response;

import java.time.LocalDateTime;

import com.sporthub.enums.SportStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SportResponse {

    private Long id;

    private String name;

    private String description;

    private String iconUrl;

    private SportStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}