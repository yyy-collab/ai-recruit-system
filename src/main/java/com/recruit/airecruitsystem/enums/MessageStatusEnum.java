package com.recruit.airecruitsystem.enums;

public enum MessageStatusEnum {
    PENDING(0, "待确认"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String desc;

    MessageStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageStatusEnum fromCode(int code) {
        for (MessageStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}