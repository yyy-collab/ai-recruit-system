package com.recruit.airecruitsystem.service.impl.seeker;

import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.dto.seeker.SeekerUpdateRequest;
import com.recruit.airecruitsystem.mapper.EmailVerifyCodeMapper;
import com.recruit.airecruitsystem.mapper.SeekerMapper;
import com.recruit.airecruitsystem.mapper.TokenBlacklistMapper;
import com.recruit.airecruitsystem.pojo.EmailVerifyCode;
import com.recruit.airecruitsystem.pojo.Seeker;
import com.recruit.airecruitsystem.pojo.TokenBlacklist;
import com.recruit.airecruitsystem.service.seeker.SeekerService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.utils.PasswordEncoder;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerInfoVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerLoginVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Service
public class SeekerServiceImpl implements SeekerService {

    @Autowired
    private SeekerMapper seekerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistMapper tokenBlacklistMapper;

    @Autowired
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

    /**
     * 求职者注册
     * @param username 用户名
     * @param password 密码（明文）
     * @return 错误码，0表示成功
     */
    @Override
    public int register(String username,String password){
        //校验用户名和密码是否为空
        if(!StringUtils.hasText(username)||!StringUtils.hasText(password)){
            return ResultCode.PARAM_ERROR;
        }

        // 2. 格式校验（用户名：5~16位字母数字下划线）
        if (!username.matches("^[a-zA-Z0-9_]{5,16}$")) {
            return ResultCode.PARAM_ERROR;
        }
        // 密码：8~16位字母数字组合
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")) {
            return ResultCode.PARAM_ERROR;
        }

        // 3. 检查用户名是否已存在
        Seeker existing = seekerMapper.findByUsername(username);
        if (existing != null) {
            return ResultCode.USERNAME_EXIST;
        }

        // 4. 加密密码
        String encodedPwd = passwordEncoder.encode(password);

        // 5. 创建Seeker对象并插入
        Seeker seeker = new Seeker();
        seeker.setUsername(username);
        seeker.setPassword(encodedPwd);
        // create_time, update_time 由数据库自动填充
        int rows = seekerMapper.insert(seeker);
        if (rows > 0) {
            return ResultCode.SUCCESS;
        } else {
            return ResultCode.PARAM_ERROR;
        }
    }

    /**
     * 求职者登录
     * @param username 用户名
     * @param password 明文密码
     * @param vo 用于封装返回数据（token、有效期、信息是否完善）
     * @return 错误码（0=成功，其他见 ResultCode）
     */
    @Override
    public int login(String username, String password, SeekerLoginVO vo) {
        //校验用户名和密码是否为1空
        if(!StringUtils.hasText(username)||!StringUtils.hasText(password)){
            return ResultCode.PARAM_ERROR;
        }

        //根据用户名查询求职者
        Seeker seeker=seekerMapper.findByUsername(username);
        if(seeker==null){
            return ResultCode.LOGIN_ERROR;//用户名错误
        }

        //密码校验
        if(!passwordEncoder.matches(password,seeker.getPassword())){
            return ResultCode.LOGIN_ERROR;//密码为空
        }

        //生成JWT Token
        String token= jwtUtil.generateToken(seeker.getId(),"seeker");
        vo.setToken(token);
        // 从工具类获取有效期（秒），保证与 token 实际有效期一致
        vo.setExpiresIn(jwtUtil.getExpiration().intValue());

        //判断个人信息是否完善(真实姓名，电话，邮箱)
        Boolean isInfoComplete=StringUtils.hasText(seeker.getRealName())
                &&StringUtils.hasText(seeker.getPhone())&&StringUtils.hasText(seeker.getEmail());
        vo.setIsInfoComplete(isInfoComplete);

        return ResultCode.SUCCESS;
    }

    @Override
    public int refreshToken(String oldToken, TokenRefreshVO vo) {
        // 1. 解析旧 Token，获取用户ID和过期时间
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

        // 2. 检查令牌是否过期，以及是否在允许刷新的窗口内
        boolean isExpired = expirationDate.before(now);
        // 计算过期前30分钟的时间点
        Date thirtyMinutesBeforeExpiry = new Date(expirationDate.getTime() - 30 * 60 * 1000);

        if (isExpired) {
            // 令牌已过期，检查是否在过期前30分钟内？实际上过期后不能刷新，接口文档要求“过期令牌无法刷新”
            // 文档原文：刷新条件：令牌过期前30分钟内可调用刷新接口，过期令牌无法刷新
            return ResultCode.TOKEN_EXPIRED;
        }

        // 3. 判断是否需要检查次数限制（如果当前时间在过期前30分钟内，则不计入次数）
        boolean isWithinGracePeriod = now.after(thirtyMinutesBeforeExpiry) && now.before(expirationDate);

        // 4. 如果需要检查次数（即不在宽限期内），则从数据库获取刷新次数信息
        Seeker seeker = seekerMapper.findById(userId);
        if (seeker == null) {
            return ResultCode.NOT_FOUND;
        }

        if (!isWithinGracePeriod) {
            // 检查24小时内刷新次数是否超过10次
            // 由于数据库只存储了最后刷新时间和累计次数，我们需要判断上次刷新是否在同一天
            // 简单实现：如果 last_refresh_time 不为空且是今天，则使用 refresh_count；否则重置计数
            LocalDateTime lastRefresh = seeker.getLastRefreshTime();
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            if (lastRefresh != null && lastRefresh.isBefore(todayStart)) {
                // 跨日，重置计数
                seekerMapper.resetRefreshCount(userId);
                seeker.setRefreshCount(0);
            }
            int refreshCount = seeker.getRefreshCount();
            if (refreshCount >= 10) {
                return ResultCode.TOO_FREQUENT;   // 操作频繁，请稍后再试
            }
        }

        // 5. 生成新 Token（角色不变）
        String role = claims.get("role", String.class);
        String newToken = jwtUtil.generateToken(userId, role);
        vo.setToken(newToken);
        vo.setExpiresIn(jwtUtil.getExpiration().intValue());

        // 6. 更新刷新次数（仅当不在宽限期内时）
        if (!isWithinGracePeriod) {
            seekerMapper.incrementRefreshCount(userId);
        }

        return ResultCode.SUCCESS;
    }

    @Override
    public SeekerInfoVO getCurrentUserInfo(Integer seekerId) {
        Seeker seeker = seekerMapper.findById(seekerId);
        if (seeker == null) {
            return null;
        }
        SeekerInfoVO vo = new SeekerInfoVO();
        vo.setId(seeker.getId());
        vo.setUsername(seeker.getUsername());
        vo.setRealName(seeker.getRealName());
        vo.setAvatarUrl(seeker.getAvatarUrl());
        vo.setPhone(seeker.getPhone());
        vo.setEmail(seeker.getEmail());
        vo.setAge(seeker.getAge());
        vo.setAddress(seeker.getAddress());
        vo.setEduBack(seeker.getEduBack());
        vo.setAlmaMater(seeker.getAlmaMater());
        vo.setState(seeker.getState());
        vo.setExPosition(seeker.getExPosition());
        vo.setExCity(seeker.getExCity());
        vo.setExSalaryMin(seeker.getExSalaryMin());
        vo.setExSalaryMax(seeker.getExSalaryMax());
        vo.setCreateTime(seeker.getCreateTime());
        vo.setUpdateTime(seeker.getUpdateTime());
        return vo;
    }

    @Override
    public int updateSeekerInfo(Integer seekerId, SeekerUpdateRequest request) {
        // 1. 检查用户是否存在
        Seeker existing = seekerMapper.findById(seekerId);
        if (existing == null) {
            return ResultCode.NOT_FOUND;
        }

        // 2. 邮箱唯一性校验
        if (request.getEmail() != null && !request.getEmail().equals(existing.getEmail())) {
            Seeker byEmail = seekerMapper.findByEmail(request.getEmail());
            if (byEmail != null && !byEmail.getId().equals(seekerId)) {
                return ResultCode.EMAIL_INVALID;   // 邮箱已被其他用户使用
            }
        }

        // 3. 将请求参数拷贝到实体对象
        Seeker updateData = new Seeker();
        updateData.setId(seekerId);
        updateData.setRealName(request.getRealName());
        updateData.setAvatarUrl(request.getAvatarUrl());
        updateData.setPhone(request.getPhone());
        updateData.setEmail(request.getEmail());
        updateData.setAge(request.getAge());
        updateData.setAddress(request.getAddress());
        updateData.setEduBack(request.getEduBack());
        updateData.setAlmaMater(request.getAlmaMater());
        updateData.setState(request.getState());
        updateData.setExPosition(request.getExPosition());
        updateData.setExCity(request.getExCity());
        updateData.setExSalaryMin(request.getExSalaryMin());
        updateData.setExSalaryMax(request.getExSalaryMax());

        // 4. 执行动态更新
        int rows = seekerMapper.updateSeeker(updateData);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }

    @Transactional
    @Override
    public int updatePassword(Integer seekerId, String oldPwd, String newPwd, String rePwd, String token) {
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
        Seeker seeker = seekerMapper.findById(seekerId);
        if (seeker == null) {
            return ResultCode.NOT_FOUND;
        }
        // 校验原密码
        if (!passwordEncoder.matches(oldPwd, seeker.getPassword())) {
            return ResultCode.OLD_PWD_ERROR;
        }

        // 加密新密码并更新数据库
        String encodedNewPwd = passwordEncoder.encode(newPwd);
        int rows = seekerMapper.updatePasswordById(seekerId, encodedNewPwd);
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
                // 记录日志，但不影响主流程（可以抛出异常，但这里选择继续）
                // log.error("Token 加入黑名单失败", e);
            }
            return ResultCode.NEED_RELOGIN;   // 通知前端需要重新登录
        }
        return ResultCode.PARAM_ERROR;
    }

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

            int rows = tokenBlacklistMapper.insert(blacklist);
            return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
        } catch (Exception e) {
            return ResultCode.TOKEN_EXPIRED; // token 无效或已过期
        }
    }

    @Override
    @Transactional
    public int deleteAccount(Integer seekerId, String password, String token) {
        // 1. 查询用户是否存在
        Seeker seeker = seekerMapper.findById(seekerId);
        if (seeker == null) {
            return ResultCode.NOT_FOUND;
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, seeker.getPassword())) {
            return ResultCode.LOGIN_ERROR;   // 密码错误
        }

        // 3. 将当前 token 加入黑名单（使令牌立即失效）
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
            // 若 token 无效（理论上不会），仍继续注销；可记录日志
            // 此处不中断流程
        }

        // 4. 删除求职者（数据库外键 ON DELETE CASCADE 自动删除简历、投递等）
        int rows = seekerMapper.deleteById(seekerId);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }

    @Override
    public int resetPassword(String username, String email, String code, String newPwd, String rePwd) {
        // 1. 新密码与确认密码一致性校验
        if (!newPwd.equals(rePwd)) {
            return ResultCode.PWD_NOT_MATCH;   // 10009
        }

        // 2. 根据用户名和邮箱查询用户（验证该用户是否存在）
        Seeker seeker = seekerMapper.selectByUsernameAndEmail(username, email);
        if (seeker == null) {
            return ResultCode.EMAIL_NOT_REGISTERED;   // 10022 邮箱未注册或不匹配
        }

        // 3. 校验验证码（查找最新且未过期的记录）
        EmailVerifyCode validCode = emailVerifyCodeMapper.selectLatestValid(email, "seeker_reset");
        if (validCode == null || !validCode.getCode().equals(code)) {
            return ResultCode.EMAIL_NOT_REGISTERED;   // 验证码错误或已过期
        }

        // 4. 验证码使用后立即删除，防止重用
        emailVerifyCodeMapper.deleteById(validCode.getId());

        // 5. 加密新密码并更新
        String encodedNewPwd = passwordEncoder.encode(newPwd);
        int rows = seekerMapper.updatePasswordById(seeker.getId(), encodedNewPwd);
        return rows > 0 ? ResultCode.SUCCESS : ResultCode.PARAM_ERROR;
    }
}
