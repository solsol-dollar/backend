package com.shinhan.eclipse.service.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SimpleLoginRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "간편 비밀번호는 6자리 숫자여야 합니다.")
        String simplePassword
) {}