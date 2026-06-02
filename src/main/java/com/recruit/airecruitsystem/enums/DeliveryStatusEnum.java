package com.recruit.airecruitsystem.enums;

public enum DeliveryStatusEnum {
    // 定义投递流程中的四种状态及其描述，方便前端展示
    PENDING(0, "待处理"),
    PASSED(1, "通过"),
    REJECTED(2, "淘汰"),
    INTERVIEW(3, "待面试");

    private Integer code;
    private String desc;

    DeliveryStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
