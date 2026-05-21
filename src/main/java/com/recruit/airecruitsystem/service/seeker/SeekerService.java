package com.recruit.airecruitsystem.service.seeker;


import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.mapper.SeekerMapper;
import com.recruit.airecruitsystem.pojo.Seeker;
import com.recruit.airecruitsystem.utils.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeekerService {

    @Autowired
    private SeekerMapper seekerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /*
     * 求职者注册
     * @param username 用户名
     * @param password 密码（明文）
     * @return 错误码，0表示成功
     */

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
}
