package com.recruit.airecruitsystem.controller.common;

import com.recruit.airecruitsystem.dto.hr.BatchDeliveryStatusUpdateDTO;
import com.recruit.airecruitsystem.dto.hr.DeliveryStatusUpdateDTO;
import com.recruit.airecruitsystem.pojo.Delivery;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.common.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    // ==========================
    // 【求职者接口】
    // ==========================

    /**
     * 1. 投递岗位（含匹配引擎）
     */
    @PostMapping("/seeker/delivery/add")
    public Result addDelivery(@RequestBody Object requestBody) {
        Integer jobId = extractJobId(requestBody);
        if (jobId == null) {
            return Result.error(10002, "岗位ID不能为空");
        }

        Delivery delivery = new Delivery();
        delivery.setJobId(jobId);
        return deliveryService.addDelivery(delivery);
    }

    private Integer extractJobId(Object requestBody) {
        if (requestBody == null) {
            return null;
        }
        if (requestBody instanceof Number) {
            return ((Number) requestBody).intValue();
        }
        if (requestBody instanceof String) {
            try {
                return Integer.valueOf((String) requestBody);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (requestBody instanceof Map) {
            Object value = ((Map<?, ?>) requestBody).get("jobId");
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.valueOf((String) value);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 2. 我的投递列表
     */
    @GetMapping("/seeker/delivery/myList")
    public Result myDeliveryList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return deliveryService.getMyDelivery(pageNum, pageSize, status);
    }

    // ==========================
    // 【HR 接口】
    // ==========================

    /**
     * 3. 查看岗位投递列表
     */
    @GetMapping("/hr/delivery/list")
    public Result hrDeliveryList(
            @RequestParam Integer jobId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sort) {
        return deliveryService.getByJobId(jobId, pageNum, pageSize, status, sort);
    }

    /**
     * 5.4 HR查看投递详情
     */
    @GetMapping("/hr/delivery/detail")
    public Result getDeliveryDetail(@RequestParam("delivery_id") Integer deliveryId) {
        return deliveryService.getDeliveryDetail(deliveryId);
    }

    /**
     * 5.5 HR 修改投递状态
     */
    @PatchMapping("/hr/delivery/updateStatus")
    public Result updateDeliveryStatus(@RequestBody DeliveryStatusUpdateDTO dto) {
        return deliveryService.updateStatus(
                dto.getDelivery_id(),
                dto.getStatus(),
                dto.getComment()
        );
    }
    /**
     * 5.6 HR 批量更新投递状态
     */
    @PatchMapping("/hr/delivery/batchUpdateStatus")
    public Result batchUpdateDeliveryStatus(@Valid @RequestBody BatchDeliveryStatusUpdateDTO dto) {
        List<Integer> successIds = deliveryService.batchUpdateStatus(dto);
        return Result.success("批量更新成功", new BatchUpdateResult(successIds));
    }

    // 辅助内部类，用于返回成功ID列表（和响应格式对齐）
    static class BatchUpdateResult {
        private List<Integer> success_ids;

        public BatchUpdateResult(List<Integer> success_ids) {
            this.success_ids = success_ids;
        }

        public List<Integer> getSuccess_ids() {
            return success_ids;
        }
    }
}