package com.webridge.scan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScanCreateRequest(
        @NotBlank(message = "검사할 웹페이지 URL을 입력하세요.")
        @Size(max = 500, message = "URL은 500자를 초과할 수 없습니다.")
        String url
) {}
