package com.recruit.airecruitsystem.service.impl.common;

import com.github.pagehelper.PageHelper;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.enums.InterviewRoundEnum;
import com.recruit.airecruitsystem.enums.InterviewTypeEnum;
import com.recruit.airecruitsystem.enums.MessageStatusEnum;
import com.recruit.airecruitsystem.mapper.*;
import com.recruit.airecruitsystem.pojo.*;
import com.recruit.airecruitsystem.dto.seeker.HandleInterviewRequest;
import com.recruit.airecruitsystem.dto.hr.SendInterviewRequest;
import com.recruit.airecruitsystem.service.common.InterviewMessageService;
import com.recruit.airecruitsystem.vo.common.PageResult;
import com.recruit.airecruitsystem.vo.hr.HrMessageDetailVO;
import com.recruit.airecruitsystem.vo.hr.HrMessageListItemVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageDetailVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageListItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterviewMessageServiceImpl implements InterviewMessageService {

    @Autowired
    private InterviewMessageMapper interviewMessageMapper;
    @Autowired
    private DeliveryMapper deliveryMapper;
    @Autowired
    private JobMapper jobMapper;
    @Autowired
    private SeekerMapper seekerMapper;
    @Autowired
    private HrMapper hrMapper;

    @Override
    @Transactional
    public int sendInterview(Integer hrId, SendInterviewRequest request) {
        // 校验面试形式与轮次
        if (InterviewTypeEnum.fromValue(request.getInterviewType()) == null ||
                InterviewRoundEnum.fromValue(request.getInterviewRound()) == null) {
            return ResultCode.PARAM_ERROR;
        }
        Delivery delivery = deliveryMapper.selectById(request.getDeliveryId());
        if (delivery == null) return ResultCode.NOT_FOUND;
        if (!Integer.valueOf(1).equals(delivery.getStatus())) {
            return ResultCode.PARAM_ERROR;
        }
        Job job = jobMapper.selectById(delivery.getJobId());
        if (job == null || !job.getHrId().equals(hrId)) return ResultCode.PARAM_ERROR;

        InterviewMessage message = InterviewMessage.builder()
                .deliveryId(request.getDeliveryId())
                .hrId(hrId)
                .seekerId(delivery.getSeekerId())
                .interviewDate(request.getInterviewDate())
                .interviewTime(request.getInterviewTime())
                .interviewType(request.getInterviewType())
                .interviewRound(request.getInterviewRound())
                .interviewAddress(request.getInterviewAddress())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .remark(request.getRemark())
                .status(MessageStatusEnum.PENDING.getCode())
                .build();
        int rows = interviewMessageMapper.insert(message);
        if (rows > 0) {
            deliveryMapper.updateStatus(request.getDeliveryId(), 3, null);
        }
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }

    @Override
    public PageResult<HrMessageListItemVO> getHrMessageList(Integer hrId, Integer status, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<InterviewMessage> messages = interviewMessageMapper.selectByHrId(hrId, status);
        // 转换为 VO
        List<HrMessageListItemVO> voList = messages.stream().map(msg -> {
            Seeker seeker = seekerMapper.findById(msg.getSeekerId()); // 改为 selectById
            String seekerName = seeker != null ? seeker.getRealName() : "";
            String jobName = "";
            Delivery delivery = deliveryMapper.selectById(msg.getDeliveryId());
            if (delivery != null) {
                Job job = jobMapper.selectById(delivery.getJobId());
                jobName = job != null ? job.getJobName() : "";
            }
            HrMessageListItemVO vo = new HrMessageListItemVO();
            vo.setMessageId(msg.getId());
            vo.setSeekerName(seekerName);
            vo.setJobName(jobName);
            vo.setStatus(msg.getStatus());
            vo.setStatusText(MessageStatusEnum.fromCode(msg.getStatus()).getDesc());
            vo.setInterviewDate(msg.getInterviewDate());
            vo.setInterviewTime(msg.getInterviewTime());
            vo.setCreateTime(msg.getCreateTime());
            vo.setUpdateTime(msg.getUpdateTime());
            vo.setRejectReason(msg.getRejectReason());
            return vo;
        }).collect(Collectors.toList());

        // 获取总记录数
        long total = 0;
        if (messages instanceof com.github.pagehelper.Page) {
            total = ((com.github.pagehelper.Page<?>) messages).getTotal();
        }
        return new PageResult<>(total, voList);
    }

    @Override
    public HrMessageDetailVO getHrMessageDetail(Integer hrId, Integer messageId) {
        if (!interviewMessageMapper.existsByHrId(messageId, hrId)) return null;
        InterviewMessage msg = interviewMessageMapper.selectById(messageId);
        if (msg == null) return null;

        HrMessageDetailVO vo = new HrMessageDetailVO();
        vo.setMessageId(msg.getId());
        vo.setStatus(msg.getStatus());
        vo.setStatusText(MessageStatusEnum.fromCode(msg.getStatus()).getDesc());
        vo.setRejectReason(msg.getRejectReason());
        vo.setCreateTime(msg.getCreateTime());
        vo.setUpdateTime(msg.getUpdateTime());

        // 求职者信息
        Seeker seeker = seekerMapper.findById(msg.getSeekerId());
        if (seeker != null) {
            HrMessageDetailVO.SeekerSimpleInfo seekerInfo = new HrMessageDetailVO.SeekerSimpleInfo();
            seekerInfo.setId(seeker.getId());
            seekerInfo.setRealName(seeker.getRealName());
            seekerInfo.setPhone(seeker.getPhone());
            seekerInfo.setEmail(seeker.getEmail());
            vo.setSeekerInfo(seekerInfo);
        }
        // 岗位信息
        Delivery delivery = deliveryMapper.selectById(msg.getDeliveryId());
        if (delivery != null) {
            Job job = jobMapper.selectById(delivery.getJobId());
            if (job != null) {
                HrMessageDetailVO.JobSimpleInfo jobInfo = new HrMessageDetailVO.JobSimpleInfo();
                jobInfo.setId(job.getId());
                jobInfo.setJobName(job.getJobName());
                vo.setJobInfo(jobInfo);
            }
        }
        // 面试信息
        HrMessageDetailVO.InterviewInfo interviewInfo = new HrMessageDetailVO.InterviewInfo();
        interviewInfo.setInterviewDate(msg.getInterviewDate());
        interviewInfo.setInterviewTime(msg.getInterviewTime());
        interviewInfo.setInterviewType(msg.getInterviewType());
        interviewInfo.setInterviewRound(msg.getInterviewRound());
        interviewInfo.setInterviewAddress(msg.getInterviewAddress());
        interviewInfo.setContactName(msg.getContactName());
        interviewInfo.setContactPhone(msg.getContactPhone());
        interviewInfo.setRemark(msg.getRemark());
        vo.setInterviewInfo(interviewInfo);
        return vo;
    }

    @Override
    public PageResult<SeekerMessageListItemVO> getSeekerMessageList(Integer seekerId, Integer status, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<InterviewMessage> messages = interviewMessageMapper.selectBySeekerId(seekerId, status);
        List<SeekerMessageListItemVO> voList = messages.stream().map(msg -> {
            Hr hr = hrMapper.selectById(msg.getHrId());
            String hrName = hr != null ? hr.getRealName() : "";
            String companyName = hr != null ? hr.getCompanyName() : "";
            String jobName = "";
            Delivery delivery = deliveryMapper.selectById(msg.getDeliveryId());
            if (delivery != null) {
                Job job = jobMapper.selectById(delivery.getJobId());
                jobName = job != null ? job.getJobName() : "";
            }
            SeekerMessageListItemVO vo = new SeekerMessageListItemVO();
            vo.setMessageId(msg.getId());
            vo.setHrName(hrName);
            vo.setCompanyName(companyName);
            vo.setJobName(jobName);
            vo.setStatus(msg.getStatus());
            vo.setStatusText(MessageStatusEnum.fromCode(msg.getStatus()).getDesc());
            vo.setInterviewDate(msg.getInterviewDate());
            vo.setInterviewTime(msg.getInterviewTime());
            vo.setCreateTime(msg.getCreateTime());
            vo.setUpdateTime(msg.getUpdateTime());
            vo.setRejectReason(msg.getRejectReason());
            return vo;
        }).collect(Collectors.toList());

        long total = 0;
        if (messages instanceof com.github.pagehelper.Page) {
            total = ((com.github.pagehelper.Page<?>) messages).getTotal();
        }
        return new PageResult<>(total, voList);
    }

    @Override
    public SeekerMessageDetailVO getSeekerMessageDetail(Integer seekerId, Integer messageId) {
        if (!interviewMessageMapper.existsBySeekerId(messageId, seekerId)) return null;
        InterviewMessage msg = interviewMessageMapper.selectById(messageId);
        if (msg == null) return null;

        SeekerMessageDetailVO vo = new SeekerMessageDetailVO();
        vo.setMessageId(msg.getId());
        vo.setStatus(msg.getStatus());
        vo.setStatusText(MessageStatusEnum.fromCode(msg.getStatus()).getDesc());
        vo.setCreateTime(msg.getCreateTime());
        vo.setUpdateTime(msg.getUpdateTime());

        Hr hr = hrMapper.selectById(msg.getHrId());
        if (hr != null) {
            SeekerMessageDetailVO.HrSimpleInfo hrInfo = new SeekerMessageDetailVO.HrSimpleInfo();
            hrInfo.setRealName(hr.getRealName());
            hrInfo.setCompanyName(hr.getCompanyName());
            hrInfo.setContactName(msg.getContactName());
            hrInfo.setContactPhone(msg.getContactPhone());
            vo.setHrInfo(hrInfo);
        }
        Delivery delivery = deliveryMapper.selectById(msg.getDeliveryId());
        if (delivery != null) {
            Job job = jobMapper.selectById(delivery.getJobId());
            if (job != null) {
                SeekerMessageDetailVO.JobSimpleInfo jobInfo = new SeekerMessageDetailVO.JobSimpleInfo();
                jobInfo.setId(job.getId());
                jobInfo.setJobName(job.getJobName());
                vo.setJobInfo(jobInfo);
            }
        }
        SeekerMessageDetailVO.InterviewInfo interviewInfo = new SeekerMessageDetailVO.InterviewInfo();
        interviewInfo.setInterviewDate(msg.getInterviewDate());
        interviewInfo.setInterviewTime(msg.getInterviewTime());
        interviewInfo.setInterviewType(msg.getInterviewType());
        interviewInfo.setInterviewRound(msg.getInterviewRound());
        interviewInfo.setInterviewAddress(msg.getInterviewAddress());
        interviewInfo.setRemark(msg.getRemark());
        vo.setInterviewInfo(interviewInfo);
        return vo;
    }

    @Override
    @Transactional
    public int handleInterview(Integer seekerId, HandleInterviewRequest request) {
        Integer messageId = request.getMessageId();
        if (!interviewMessageMapper.existsBySeekerId(messageId, seekerId)) {
            return ResultCode.INTERVIEW_NOT_EXIST;
        }
        InterviewMessage msg = interviewMessageMapper.selectById(messageId);
        if (msg == null) return ResultCode.INTERVIEW_NOT_EXIST;
        if (msg.getStatus() != MessageStatusEnum.PENDING.getCode()) {
            return ResultCode.INTERVIEW_ALREADY_HANDLED;
        }
        if (request.getStatus() == MessageStatusEnum.REJECTED.getCode() && !StringUtils.hasText(request.getRejectReason())) {
            return ResultCode.REJECT_REASON_EMPTY;
        }
        int rows = interviewMessageMapper.updateStatus(messageId, request.getStatus(), request.getRejectReason());
        if (rows > 0 && request.getStatus() == MessageStatusEnum.REJECTED.getCode()) {
            deliveryMapper.updateStatus(msg.getDeliveryId(), 2, request.getRejectReason());
        }
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }
}