package com.recruit.airecruitsystem.dto.hr;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeliveryStatusUpdateDTO {

    @NotNull(message = "投递ID不能为空")
    private Integer delivery_id;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(2)
    private Integer status;

    private String comment;

    public Integer getDelivery_id() {
        return delivery_id;
    }

    public void setDelivery_id(Integer delivery_id) {
        this.delivery_id = delivery_id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}