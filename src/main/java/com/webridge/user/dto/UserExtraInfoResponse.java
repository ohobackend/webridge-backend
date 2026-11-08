package com.webridge.user.dto;

import com.webridge.user.entity.User;

public record UserExtraInfoResponse(
    String pathToWebridge,
    String pathToWebridgeOther,
    String occupation,
    String occupationOther
) {
    public static UserExtraInfoResponse from(User user) {
        return new UserExtraInfoResponse(
            user.getPathToWebridge(),
            user.getPathToWebridgeOther(),
            user.getOccupation(),
            user.getOccupationOther()
        );
    }
}
