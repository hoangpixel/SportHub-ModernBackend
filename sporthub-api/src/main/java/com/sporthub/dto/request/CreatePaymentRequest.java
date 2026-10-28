package com.sporthub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreatePaymentRequest {

    @NotNull(message = "Mã booking không được để trống")
    @Positive
    private Long bookingId;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
}
