package com.webridge.user;

import com.webridge.auth.dto.AuthUser;
import com.webridge.user.dto.UserDeleteResponse;
import com.webridge.user.dto.UserExtraInfoRequest;
import com.webridge.user.dto.UserExtraInfoResponse;
import com.webridge.user.dto.UserProfileResponse;
import com.webridge.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 프로필 및 추가 정보 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(userService.getProfile(authUser.getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 이름 정보를 수정합니다.")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(authUser.getId(), request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다.")
    public ResponseEntity<UserDeleteResponse> deleteAccount(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(userService.deleteUser(authUser.getId()));
    }

    @PostMapping({"/extra-info", "/extra-info/"})
    @Operation(summary = "사용자 추가 정보 입력/수정", description = "사용자의 유입 경로 및 직업 정보를 저장합니다.")
    public ResponseEntity<UserExtraInfoResponse> updateExtraInfo(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody UserExtraInfoRequest request
    ) {
        return ResponseEntity.ok(userService.updateExtraInfo(authUser.getId(), request));
    }
}
