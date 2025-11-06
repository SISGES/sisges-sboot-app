package com.unileste.sisges.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRoleENUM {
    STUDENT(0),
    TEACHER(1),
    ADMIN(2),
    DEV_ADMIN(3);

    private final Integer code;

    public static UserRoleENUM fromCode(int cd) {
        for (UserRoleENUM userRoleENUM : values()) {
            if (userRoleENUM.getCode() == cd) {
                return userRoleENUM;
            }
        }

        throw new IllegalArgumentException("No enum found for code: " + cd);
    }

}