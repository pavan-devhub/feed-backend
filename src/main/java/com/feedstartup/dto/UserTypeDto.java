package com.feedstartup.dto;

import com.feedstartup.model.UserType;

/** One option of the registration form's "User Type" dropdown. */
public record UserTypeDto(String name) {

    public static UserTypeDto from(UserType type) {
        return new UserTypeDto(type.getName());
    }
}
