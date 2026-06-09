package com.recruit.airecruitsystem.service.common;

import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.dto.hr.SendInterviewRequest;
import com.recruit.airecruitsystem.dto.seeker.HandleInterviewRequest;
import com.recruit.airecruitsystem.vo.common.PageResult;
import com.recruit.airecruitsystem.vo.hr.HrMessageDetailVO;
import com.recruit.airecruitsystem.vo.hr.HrMessageListItemVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageDetailVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageListItemVO;

public interface InterviewMessageService {
    int sendInterview(Integer hrId, SendInterviewRequest request);

    PageResult<HrMessageListItemVO> getHrMessageList(Integer hrId, Integer status, int pageNum, int pageSize);

    HrMessageDetailVO getHrMessageDetail(Integer hrId, Integer messageId);

    PageResult<SeekerMessageListItemVO> getSeekerMessageList(Integer seekerId, Integer status, int pageNum, int pageSize);

    SeekerMessageDetailVO getSeekerMessageDetail(Integer seekerId, Integer messageId);

    int handleInterview(Integer seekerId, HandleInterviewRequest request);
}