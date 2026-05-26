-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_recruit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_recruit;

-- ----------------------------
-- 1. 求职者表 (seeker)
-- ----------------------------
CREATE TABLE `seeker`(
                          `id` INT NOT NULL AUTO_INCREMENT COMMENT '求职者ID',
                          `username` VARCHAR(16) NOT NULL COMMENT '用户名，5~16位字母数字下划线',
                          `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
                          `real_name` VARCHAR(20) DEFAULT NULL COMMENT '真实姓名，2~20字符',
                          `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                          `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号，11位',
                          `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱，唯一',
                          `age` INT DEFAULT NULL COMMENT '年龄，16~65',
                          `address` VARCHAR(200) DEFAULT NULL COMMENT '居住地址',
                          `edu_back` ENUM('专科','本科','硕士','博士') DEFAULT NULL COMMENT '学历',
                          `alma_mater` VARCHAR(100) DEFAULT NULL COMMENT '毕业院校',
                          `state` ENUM('在职','离职','应届毕业生') DEFAULT NULL COMMENT '求职状态',
                          `ex_position` VARCHAR(50) DEFAULT NULL COMMENT '期望职位',
                          `ex_city` VARCHAR(50) DEFAULT NULL COMMENT '期望城市',
                          `ex_salary_min` INT DEFAULT NULL COMMENT '期望最低薪资(K)',
                          `ex_salary_max` INT DEFAULT NULL COMMENT '期望最高薪资(K)',
                          `refresh_count` INT DEFAULT 0 COMMENT '当日刷新JWT次数(24h内)',
                          `last_refresh_time` DATETIME DEFAULT NULL COMMENT '上次刷新时间',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
                          `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_username` (`username`),
                          UNIQUE KEY `uk_email` (`email`),
                          KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职者表';

-- ----------------------------
-- 2. HR 用户表 (hr)
-- ----------------------------
CREATE TABLE `hr` (
                      `id` INT NOT NULL AUTO_INCREMENT COMMENT 'HR ID',
                      `username` VARCHAR(16) NOT NULL COMMENT '用户名',
                      `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
                      `real_name` VARCHAR(20) DEFAULT NULL COMMENT '真实姓名',
                      `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                      `company_name` VARCHAR(100) DEFAULT NULL COMMENT '公司名称',
                      `email` VARCHAR(100) DEFAULT NULL COMMENT '企业邮箱',
                      `phone` VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
                      `refresh_count` INT DEFAULT 0 COMMENT '当日刷新JWT次数',
                      `last_refresh_time` DATETIME DEFAULT NULL,
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_username` (`username`),
                      UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR用户表';

-- ----------------------------
-- 3. 岗位表 (job)
-- ----------------------------
CREATE TABLE `job` (
                       `id` INT NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
                       `hr_id` INT NOT NULL COMMENT '发布HR的ID',
                       `job_name` VARCHAR(100) NOT NULL COMMENT '岗位名称',
                       `job_desc` TEXT NOT NULL COMMENT '岗位描述',
                       `requirement` TEXT NOT NULL COMMENT '任职要求',
                       `keywords` VARCHAR(255) NOT NULL COMMENT '核心关键词，逗号分隔',
                       `salary` VARCHAR(20) DEFAULT NULL COMMENT '薪资范围，如15-25K',
                       `work_address` VARCHAR(200) DEFAULT NULL COMMENT '工作地点',
                       `work_experience` VARCHAR(20) DEFAULT NULL COMMENT '工作经验要求，如3-5年',
                       `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-上线，0-下线',
                       `delivery_count` INT DEFAULT 0 COMMENT '投递数量（冗余统计）',
                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                       `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (`id`),
                       KEY `idx_hr_id` (`hr_id`),
                       KEY `idx_status` (`status`),
                       FULLTEXT KEY `ft_job_content` (`job_name`,`job_desc`,`requirement`),
                       CONSTRAINT `fk_job_hr` FOREIGN KEY (`hr_id`) REFERENCES `hr` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- ----------------------------
-- 4. 简历表 (resume)
-- ----------------------------
CREATE TABLE `resume` (
                          `id` INT NOT NULL AUTO_INCREMENT COMMENT '简历ID',
                          `seeker_id` INT NOT NULL COMMENT '求职者ID',
                          `file_name` VARCHAR(200) NOT NULL COMMENT '原文件名',
                          `file_url` VARCHAR(500) NOT NULL COMMENT '文件存储URL',
                          `is_parsed` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未解析，1-解析成功，2-解析中，3-解析失败',
                          `parse_fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '解析失败原因',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_seeker_id` (`seeker_id`),
                          CONSTRAINT `fk_resume_seeker` FOREIGN KEY (`seeker_id`) REFERENCES `seeker` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表，每个求职者最多一条';

-- ----------------------------
-- 5. AI简历解析结果表 (resume_parse_result)
-- ----------------------------
CREATE TABLE `resume_parse_result` (
                                       `id` INT NOT NULL AUTO_INCREMENT,
                                       `resume_id` INT NOT NULL COMMENT '简历ID',
                                       `keyword_coverage` INT DEFAULT NULL COMMENT '关键词覆盖率',
                                       `basic_info` JSON DEFAULT NULL COMMENT '基本信息JSON：real_name,phone,email,age,edu_back,alma_mater',
                                       `work_experience` VARCHAR(20) DEFAULT NULL COMMENT '工作年限',
                                       `skills` JSON DEFAULT NULL COMMENT '技能列表，JSON数组',
                                       `work_history` JSON DEFAULT NULL COMMENT '工作经历，JSON数组，每项含company,position,start_time,end_time,description,core_skills',
                                       `ai_summary` TEXT COMMENT 'AI总结',
                                       `improvement_suggestions` TEXT COMMENT '改进建议',
                                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_resume_id` (`resume_id`),
                                       CONSTRAINT `fk_parse_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历AI解析结果';

-- ----------------------------
-- 6. 投递记录表 (delivery)
-- ----------------------------
CREATE TABLE `delivery` (
                            `id` INT NOT NULL AUTO_INCREMENT COMMENT '投递ID',
                            `job_id` INT NOT NULL COMMENT '岗位ID',
                            `seeker_id` INT NOT NULL COMMENT '求职者ID',
                            `resume_id` INT NOT NULL COMMENT '投递时的简历ID（快照）',
                            `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待处理，1-通过，2-淘汰，3-待面试',
                            `hr_comment` VARCHAR(500) DEFAULT NULL COMMENT 'HR备注',
                            `delivery_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
                            `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_job_seeker` (`job_id`,`seeker_id`),
                            KEY `idx_seeker_id` (`seeker_id`),
                            KEY `idx_job_id` (`job_id`),
                            KEY `idx_status` (`status`),
                            CONSTRAINT `fk_delivery_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`) ON DELETE CASCADE,
                            CONSTRAINT `fk_delivery_seeker` FOREIGN KEY (`seeker_id`) REFERENCES `seeker` (`id`) ON DELETE CASCADE,
                            CONSTRAINT `fk_delivery_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`) ON DELETE RESTRICT  -- 限制：有投递的简历不可删除
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递记录表';

-- ----------------------------
-- 7. AI匹配结果表 (ai_match_result)
-- ----------------------------
CREATE TABLE `ai_match_result` (
                                   `id` INT NOT NULL AUTO_INCREMENT,
                                   `delivery_id` INT NOT NULL COMMENT '投递ID',
                                   `match_score` DECIMAL(5,1) NOT NULL COMMENT '匹配分数 0-100',
                                   `match_level` ENUM('极高潜力','高潜力','中等潜力','低潜力') NOT NULL,
                                   `job_keywords` JSON DEFAULT NULL COMMENT '岗位关键词数组',
                                   `resume_keywords` JSON DEFAULT NULL COMMENT '简历关键词数组',
                                   `match_detail` TEXT COMMENT '匹配详情',
                                   `core_advantages` TEXT COMMENT '核心优势',
                                   `potential_risks` TEXT COMMENT '潜在风险',
                                   `skill_tags` VARCHAR(255) DEFAULT NULL COMMENT '技能标签',
                                   `analysis_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
                                   `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_delivery_id` (`delivery_id`),
                                   CONSTRAINT `fk_match_delivery` FOREIGN KEY (`delivery_id`) REFERENCES `delivery` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI匹配结果';

-- ----------------------------
-- 8. 面试邀请消息表 (interview_message)
-- ----------------------------
CREATE TABLE `interview_message` (
                                     `id` INT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                     `delivery_id` INT NOT NULL COMMENT '关联投递ID',
                                     `hr_id` INT NOT NULL COMMENT '发送方HR ID',
                                     `seeker_id` INT NOT NULL COMMENT '接收方求职者ID',
                                     `interview_date` DATE NOT NULL COMMENT '面试日期',
                                     `interview_time` TIME NOT NULL COMMENT '面试时间',
                                     `interview_type` VARCHAR(20) NOT NULL COMMENT '面试形式',
                                     `interview_round` VARCHAR(20) NOT NULL COMMENT '面试轮次',
                                     `interview_address` VARCHAR(255) NOT NULL COMMENT '面试地点/链接',
                                     `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人',
                                     `contact_phone` VARCHAR(11) NOT NULL COMMENT '联系人电话',
                                     `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                     `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待确认，1-已接受，2-已拒绝',
                                     `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
                                     `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_delivery_id` (`delivery_id`),
                                     KEY `idx_hr_id` (`hr_id`),
                                     KEY `idx_seeker_id` (`seeker_id`),
                                     KEY `idx_status` (`status`),
                                     CONSTRAINT `fk_msg_delivery` FOREIGN KEY (`delivery_id`) REFERENCES `delivery` (`id`) ON DELETE CASCADE,
                                     CONSTRAINT `fk_msg_hr` FOREIGN KEY (`hr_id`) REFERENCES `hr` (`id`),
                                     CONSTRAINT `fk_msg_seeker` FOREIGN KEY (`seeker_id`) REFERENCES `seeker` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试邀请消息表';

-- ----------------------------
-- 9. 黑名单/令牌黑名单表（用于登出/注销使令牌失效，可选）
-- ----------------------------
CREATE TABLE `token_blacklist` (
                                   `id` INT NOT NULL AUTO_INCREMENT,
                                   `token` VARCHAR(500) NOT NULL COMMENT 'JWT令牌',
                                   `expire_time` DATETIME NOT NULL COMMENT '令牌过期时间',
                                   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_token` (`token`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWT黑名单';

-- ----------------------------
-- 10. 邮箱验证码表（用于重置密码）
-- ----------------------------
CREATE TABLE `email_verify_code` (
                                     `id` INT NOT NULL AUTO_INCREMENT,
                                     `email` VARCHAR(100) NOT NULL,
                                     `code` VARCHAR(6) NOT NULL,
                                     `type` VARCHAR(20) NOT NULL COMMENT 'seeker_reset, hr_reset',
                                     `expire_time` DATETIME NOT NULL,
                                     `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_email_type` (`email`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码';

-- ----------------------------
-- 索引优化补充（按需）
-- ----------------------------
-- 为 job 表增加 hr_id+status 联合索引，便于HR查询自己的岗位
ALTER TABLE `job` ADD INDEX `idx_hr_status` (`hr_id`, `status`);
-- 为 delivery 表增加 job_id+status 联合索引，便于HR按岗位筛选投递
ALTER TABLE `delivery` ADD INDEX `idx_job_status` (`job_id`, `status`);
-- 为 delivery 表增加 seeker_id+status 联合索引，便于求职者筛选投递
ALTER TABLE `delivery` ADD INDEX `idx_seeker_status` (`seeker_id`, `status`);
