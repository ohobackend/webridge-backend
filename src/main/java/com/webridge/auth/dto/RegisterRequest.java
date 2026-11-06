package com.webridge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    String password,

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    String password2,

    @NotBlank(message = "이름을 입력해주세요.")
    String name,

    @NotNull(message = "서비스 이용약관에 동의해야 합니다.")
    Boolean serviceTermsAgreed,

    @NotNull(message = "만 14세 이상 확인에 동의해야 합니다.")
    Boolean ageVerificationAgreed,

    @NotNull(message = "개인정보 수집 및 이용에 동의해야 합니다.")
    Boolean privacyPolicyAgreed,

    Boolean marketingAgreed
) {}
