package com.recruit.airecruitsystem.service.seeker;

import com.recruit.airecruitsystem.dto.seeker.SeekerUpdateRequest;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerInfoVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerLoginVO;

/**
 * 求职者业务逻辑接口
 */

public interface SeekerService {

    //求职者注册
    int register(String username,String password);

    /*
    * @param vo 用于封装返回数据（token、有效期、信息是否完善)
    * */
    int login(String username, String password, SeekerLoginVO vo);

    //刷新令牌
    int refreshToken(String oldToken, TokenRefreshVO vo);

    //获取用户详情信息
    SeekerInfoVO getCurrentUserInfo(Integer seekerId);

    //更新求职者信息
    int updateSeekerInfo(Integer seekerId, SeekerUpdateRequest request);

    //更新密码
    int updatePassword(Integer seekerId, String oldPwd, String newPwd, String rePwd,String token);

    //用户登出(将token加入黑名单)
    int logout(String token);

    //注销账号
    int deleteAccount(Integer seekerId, String password, String token);

    //重置密码
    int resetPassword(String username, String email, String code, String newPwd, String rePwd);
}
