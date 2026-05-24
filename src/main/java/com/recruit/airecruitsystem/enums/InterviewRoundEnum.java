package com.recruit.airecruitsystem.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InterviewRoundEnum {
    FIRST("初试"),
    SECOND("复试"),
    FINAL("终试");

    private final String value;

    InterviewRoundEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static InterviewRoundEnum fromValue(String value) {
        for (InterviewRoundEnum round : values()) {
            if (round.value.equals(value)) {
                return round;
            }
        }
        return null;
    }
}