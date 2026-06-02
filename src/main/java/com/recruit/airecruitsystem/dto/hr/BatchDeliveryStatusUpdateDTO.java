package com.recruit.airecruitsystem.dto.hr;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BatchDeliveryStatusUpdateDTO {

    @NotEmpty(message = "投递ID列表不能为空")
    private List<Integer> delivery_ids;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(2)
    private Integer status;

    private String reject_reason;

    public List<Integer> getDelivery_ids() {
        return delivery_ids;
    }

    public void setDelivery_ids(List<Integer> delivery_ids) {
        this.delivery_ids = delivery_ids;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReject_reason() {
        return reject_reason;
    }

    public void setReject_reason(String reject_reason) {
        this.reject_reason = reject_reason;
    }
}