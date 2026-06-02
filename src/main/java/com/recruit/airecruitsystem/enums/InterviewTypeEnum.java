package com.recruit.airecruitsystem.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InterviewTypeEnum {
    ONLINE("线上面试"),
    OFFLINE("现场面试"),
    PHONE("电话面试");

    private final String value;

    InterviewTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static InterviewTypeEnum fromValue(String value) {
        for (InterviewTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}