package com.sporthub.dto.request;

import com.sporthub.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Status không được để trống")
    private UserStatus status;
}