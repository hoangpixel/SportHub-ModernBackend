package com.sporthub.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        Map<String, String> errors
) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}