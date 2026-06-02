package com.recruit.airecruitsystem.service.common;

import com.recruit.airecruitsystem.dto.hr.BatchDeliveryStatusUpdateDTO;
import com.recruit.airecruitsystem.pojo.Delivery;
import com.recruit.airecruitsystem.result.Result;

import java.util.List;

public interface DeliveryService {

    // 1. 投递岗位
    Result addDelivery(Delivery delivery);

    // 2. 求职者查看自己的投递列表（分页+状态筛选）
    Result getMyDelivery(Integer pageNum, Integer pageSize, Integer status);

    // 3. HR查看岗位下所有投递（分页+状态筛选+排序）
    Result getByJobId(Integer jobId, Integer pageNum, Integer pageSize, Integer status, String sort);

    // 4.HR查看简历
    Result getDeliveryDetail(Integer deliveryId);
    // 5. HR修改投递状态
    Result updateStatus(Integer deliveryId, Integer status, String comment);
    // 6.HR批量修改状态
    List<Integer> batchUpdateStatus(BatchDeliveryStatusUpdateDTO dto);

}