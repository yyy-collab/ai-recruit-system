package com.recruit.airecruitsystem.service.impl.hr;

import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.mapper.EmailVerifyCodeMapper;
import com.recruit.airecruitsystem.mapper.HrMapper;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.mapper.TokenBlacklistMapper;
import com.recruit.airecruitsystem.pojo.EmailVerifyCode;
import com.recruit.airecruitsystem.pojo.Hr;
import com.recruit.airecruitsystem.pojo.TokenBlacklist;
import com.recruit.airecruitsystem.dto.hr.HrUpdateRequest;
import com.recruit.airecruitsystem.vo.hr.HrInfoVO;
import com.recruit.airecruitsystem.vo.hr.HrLoginVO;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;
import com.recruit.airecruitsystem.service.hr.HrService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.utils.PasswordEncoder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * HR 业务逻辑实现类
 * 提供注册、登录、刷新Token、信息管理、密码修改、登出、重置密码等功能
 */
@Slf4j
@Service
public class HrServiceImpl implements HrService {

    @Autowired
    private HrMapper hrMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistMapper tokenBlacklistMapper;

    @Autowired
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

    @Autowired
    private JobMapper jobMapper;

    /**
     * HR 注册
     * @param username 用户名
     * @param password 明文密码
     * @return 错误码
     */
    @Override
    public int register(String username, String password) {
        // 参数非空校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return ResultCode.PARAM_ERROR;
        }
        // 用户名格式：5~16位字母数字下划线
        if (!username.matches("^[a-zA-Z0-9_]{5,16}$")) {
            return ResultCode.PARAM_ERROR;
        }
        // 密码格式：8~16位必须包含字母和数字
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")) {
            return ResultCode.PARAM_ERROR;
        }
        // 检查用户名是否已存在
        Hr existing = hrMapper.selectByUsername(username);
        if (existing != null) {
            return ResultCode.USERNAME_EXIST;
        }
        // 创建HR对象，密码加密存储
        Hr hr = Hr.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();
        int rows = hrMapper.insert(hr);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }

    /**
     * HR 登录
     * @param username 用户名
     * @param password 明文密码
     * @param vo 用于返回 token、有效期、信息完善标志
     * @return 错误码
     */
    @Override
    public int login(String username, String password, HrLoginVO vo) {
        // 非空校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return ResultCode.PARAM_ERROR;
        }
        // 查询用户
        Hr hr = hrMapper.selectByUsername(username);
        if (hr == null || !passwordEncoder.matches(password, hr.getPassword())) {
            return ResultCode.LOGIN_ERROR;
        }
        // 生成 JWT token，角色为 "hr"
        String token = jwtUtil.generateToken(hr.getId(), "hr");
        vo.setToken(token);
        vo.setExpiresIn(jwtUtil.getExpiration().intValue());

        // 判断信息是否完善：公司名称、真实姓名、电话、邮箱 全部不为空才算完善
        boolean infoComplete = StringUtils.hasText(hr.getCompanyName())
                && StringUtils.hasText(hr.getRealName())
                && StringUtils.hasText(hr.getPhone())
                && StringUtils.hasText(hr.getEmail());
        vo.setIsInfoComplete(infoComplete);
        return ResultCode.SUCCESS;
    }

    /**
     * 刷新 JWT Token
     * @param oldToken 原令牌
     * @param vo 用于返回新 token 和有效期
     * @return 错误码
     */
    @Override
    public int refreshToken(String oldToken, TokenRefreshVO vo) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(oldToken);
        } catch (ExpiredJwtException e) {
            return ResultCode.TOKEN_EXPIRED;
        } catch (Exception e) {
            return ResultCode.TOKEN_EXPIRED;
        }
        Integer userId = claims.get("userId", Integer.class);
        Date expirationDate = claims.getExpiration();
        Date now = new Date();
        // 检查令牌是否已过期（不允许刷新已过期的令牌）
        if (expirationDate.before(now)) {
            return ResultCode.TOKEN_EXPIRED;
        }
        // 计算过期前30分钟的时间点
        Date thirtyMinutesBeforeExpiry = new Date(expirationDate.getTime() - 30 * 60 * 1000);
        boolean isWithinGracePeriod = now.after(thirtyMinutesBeforeExpiry) && now.before(expirationDate);

        Hr hr = hrMapper.selectById(userId);
        if (hr == null) {
            return ResultCode.NOT_FOUND;
        }
        // 如果不在宽限期内，需要检查24小时内刷新次数（最多10次）
        if (!isWithinGracePeriod) {
            LocalDateTime lastRefresh = hr.getLastRefreshTime();
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            // 如果上次刷新时间在今日之前，重置计数
            if (lastRefresh != null && lastRefresh.isBefore(todayStart)) {
                hrMapper.resetRefreshCount(userId);
                hr.setRefreshCount(0);
            }
            if (hr.getRefreshCount() >= 10) {
                return ResultCode.TOO_FREQUENT;
            }
        }
        // 生成新令牌（角色沿用原令牌中的角色）
        String role = claims.get("role", String.class);
        String newToken = jwtUtil.generateToken(userId, role);
        vo.setToken(newToken);
        vo.setExpiresIn(jwtUtil.getExpiration().intValue());

        // 仅当不在宽限期内时，增加刷新次数
        if (!isWithinGracePeriod) {
            hrMapper.incrementRefreshCount(userId);
        }
        return ResultCode.SUCCESS;
    }

    /**
     * 获取当前 HR 详细信息
     * @param hrId HR ID
     * @return HR 信息 VO，不存在返回 null
     */
    @Override
    public HrInfoVO getCurrentHrInfo(Integer hrId) {
        Hr hr = hrMapper.selectById(hrId);
        if (hr == null) return null;
        HrInfoVO vo = new HrInfoVO();
        vo.setId(hr.getId());
        vo.setUsername(hr.getUsername());
        vo.setRealName(hr.getRealName());
        vo.setAvatarUrl(hr.getAvatarUrl());
        vo.setCompanyName(hr.getCompanyName());
        vo.setEmail(hr.getEmail());
        vo.setPhone(hr.getPhone());
        vo.setCreateTime(hr.getCreateTime());
        vo.setUpdateTime(hr.getUpdateTime());
        return vo;
    }

    /**
     * 更新 HR 信息（部分更新）
     * @param hrId HR ID
     * @param request 更新请求 DTO
     * @return 错误码
     */
    @Override
    public int updateHrInfo(Integer hrId, HrUpdateRequest request) {
        Hr existing = hrMapper.selectById(hrId);
        if (existing == null) return ResultCode.NOT_FOUND;

        // 如果更新邮箱，需校验邮箱唯一性
        if (request.getEmail() != null && !request.getEmail().equals(existing.getEmail())) {
            Hr byEmail = hrMapper.selectByEmail(request.getEmail());
            if (byEmail != null && !byEmail.getId().equals(hrId)) {
                return ResultCode.EMAIL_INVALID;
            }
        }

        Hr updateData = new Hr();
        updateData.setId(hrId);
        updateData.setRealName(request.getRealName());
        updateData.setAvatarUrl(request.getAvatarUrl());
        updateData.setCompanyName(request.getCompanyName());
        updateData.setEmail(request.getEmail());
        updateData.setPhone(request.getPhone());

        int rows = hrMapper.updateHr(updateData);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }

    /**
     * 修改密码（已登录）
     * @param hrId HR ID
     * @param oldPwd 原密码
     * @param newPwd 新密码
     * @param rePwd 确认新密码
     * @return 错误码
     */
    @Transactional
    @Override
    public int updatePassword(Integer hrId, String oldPwd, String newPwd, String rePwd, String token) {
        // 非空校验
        if (oldPwd == null || newPwd == null || rePwd == null) {
            return ResultCode.PARAM_ERROR;
        }
        // 新密码与确认密码一致性
        if (!newPwd.equals(rePwd)) {
            return ResultCode.PWD_NOT_MATCH;
        }
        // 新密码格式校验
        if (!newPwd.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")) {
            return ResultCode.PARAM_ERROR;
        }
        // 查询用户
        Hr hr = hrMapper.selectById(hrId);
        if (hr == null) {
            return ResultCode.NOT_FOUND;
        }
        // 校验原密码
        if (!passwordEncoder.matches(oldPwd, hr.getPassword())) {
            return ResultCode.OLD_PWD_ERROR;
        }

        // 加密并更新密码
        String encodedNewPwd = passwordEncoder.encode(newPwd);
        int rows = hrMapper.updatePasswordById(hrId, encodedNewPwd);
        if (rows > 0) {
            // 将当前 Token 加入黑名单，使其立即失效
            try {
                Claims claims = jwtUtil.parseToken(token);
                Date expirationDate = claims.getExpiration();
                LocalDateTime expireTime = expirationDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                TokenBlacklist blacklist = TokenBlacklist.builder()
                        .token(token)
                        .expireTime(expireTime)
                        .build();
                tokenBlacklistMapper.insert(blacklist);
            } catch (Exception e) {
                // 记录日志，不中断流程
            }
            return ResultCode.NEED_RELOGIN;
        }
        return ResultCode.PARAM_ERROR;
    }

    /**
     * 登出：将当前 token 加入黑名单
     * @param token JWT 令牌
     * @return 错误码
     */
    @Transactional
    @Override
    public int logout(String token) {
        try {
            // 解析 token 获取过期时间
            Date expirationDate = jwtUtil.parseToken(token).getExpiration();
            LocalDateTime expireTime = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            // 构建黑名单记录
            TokenBlacklist blacklist = TokenBlacklist.builder()
                    .token(token)
                    .expireTime(expireTime)
                    .build();
            tokenBlacklistMapper.insert(blacklist);
            return ResultCode.SUCCESS;
        } catch (Exception e) {
            return ResultCode.TOKEN_EXPIRED;
        }
    }

    /**
     * 重置密码（未登录，通过邮箱验证码）
     * @param username 用户名
     * @param email 邮箱
     * @param code 验证码
     * @param newPwd 新密码
     * @param rePwd 确认密码
     * @return 错误码
     */
    @Override
    public int resetPassword(String username, String email, String code, String newPwd, String rePwd) {
        // 新密码与确认密码一致性
        if (!newPwd.equals(rePwd)) {
            return ResultCode.PWD_NOT_MATCH;
        }
        // 根据用户名和邮箱查询 HR
        Hr hr = hrMapper.selectByUsernameAndEmail(username, email);
        if (hr == null) {
            return ResultCode.EMAIL_NOT_REGISTERED;
        }
        // 查询有效验证码（类型为 "hr_reset"）
        EmailVerifyCode validCode = emailVerifyCodeMapper.selectLatestValid(email, "hr_reset");
        if (validCode == null || !validCode.getCode().equals(code)) {
            return ResultCode.EMAIL_NOT_REGISTERED;
        }
        // 验证码使用后立即删除，防止重用
        emailVerifyCodeMapper.deleteById(validCode.getId());
        // 加密并更新密码
        String encodedNewPwd = passwordEncoder.encode(newPwd);
        int rows = hrMapper.updatePasswordById(hr.getId(), encodedNewPwd);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }


    @Override
    @Transactional
    public int deleteAccount(Integer hrId, String password, String token) {
        // 1. 查询 HR 是否存在
        Hr hr = hrMapper.selectById(hrId);
        if (hr == null) {
            return ResultCode.NOT_FOUND;
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, hr.getPassword())) {
            return ResultCode.LOGIN_ERROR;
        }

        // 3. 业务校验：是否存在上线状态的岗位（建议先下线岗位或无法注销）
        int onlineJobs = hrMapper.countOnlineJobs(hrId);
        if (onlineJobs > 0) {
            return ResultCode.HR_HAS_ONLINE_JOBS;   // 存在上线岗位，不允许注销（可提示请先下线所有岗位）
        }

        // 4. 将当前 Token 加入黑名单
        try {
            Date expirationDate = jwtUtil.parseToken(token).getExpiration();
            LocalDateTime expireTime = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            TokenBlacklist blacklist = TokenBlacklist.builder()
                    .token(token)
                    .expireTime(expireTime)
                    .build();
            tokenBlacklistMapper.insert(blacklist);
        } catch (Exception e) {
            // 记录日志，不中断注销流程
            log.error("HR注销时Token加入黑名单失败", e);
        }

        // 5. 删除 HR（外键级联删除岗位、投递等）
        int rows = hrMapper.deleteById(hrId);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }
}