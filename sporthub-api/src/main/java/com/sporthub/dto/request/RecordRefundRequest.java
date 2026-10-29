package com.sporthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordRefundRequest {
    @NotBlank
    @Size(max = 200)
    private String reference;
}
