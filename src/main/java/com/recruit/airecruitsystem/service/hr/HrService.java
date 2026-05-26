package com.recruit.airecruitsystem.service.hr;

import com.recruit.airecruitsystem.dto.hr.HrUpdateRequest;
import com.recruit.airecruitsystem.vo.hr.HrInfoVO;
import com.recruit.airecruitsystem.vo.hr.HrLoginVO;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;

public interface HrService {
    //HR注册
    int register(String username, String password);

    //HR登录
    int login(String username, String password, HrLoginVO vo);

    //刷新token
    int refreshToken(String oldToken, TokenRefreshVO vo);

    //获得当前HR的信息
    HrInfoVO getCurrentHrInfo(Integer hrId);

    //更新信息
    int updateHrInfo(Integer hrId, HrUpdateRequest request);

    //更新密码
    int updatePassword(Integer hrId, String oldPwd, String newPwd, String rePwd ,String token);

    //登出
    int logout(String token);

    //重置密码
    int resetPassword(String username, String email, String code, String newPwd, String rePwd);

    //注销账号
    int deleteAccount(Integer hrId, String password, String token);
}