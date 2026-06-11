USE ai_recruit;

SET @demo_password = '$2a$10$357gZxrX2wPHq6KBbJ.W2ebA/Z13UZIEjUR6hBoa8D3MwDDBypaOK';

INSERT INTO hr (username, password, real_name, avatar_url, company_name, email, phone, create_time, update_time)
VALUES
  ('demo_hr1', @demo_password, '林秋雅', NULL, '腾讯科技', 'hr.tencent@example.com', '13800000000', NOW(), NOW()),
  ('demo_hr2', @demo_password, '周明轩', NULL, '阿里巴巴', 'hr.alibaba@example.com', '13800000001', NOW(), NOW()),
  ('demo_hr3', @demo_password, '沈清扬', NULL, '百度智能云', 'hr.baidu@example.com', '13800000002', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  company_name = VALUES(company_name),
  email = VALUES(email),
  phone = VALUES(phone),
  update_time = NOW();

INSERT INTO seeker (username, password, real_name, avatar_url, phone, email, age, address, edu_back, alma_mater, state, ex_position, ex_city, ex_salary_min, ex_salary_max, create_time, update_time)
VALUES
  ('demo_sk1', @demo_password, '陈志远', NULL, '13900000001', 'chen.zhiyuan@example.com', 29, '杭州西湖区', '本科', '浙江大学', '在职', '高级前端工程师/AI产品架构', '杭州', 25, 40, NOW(), NOW()),
  ('demo_sk2', @demo_password, '李雪莹', NULL, '13900000002', 'li.xueying@example.com', 26, '上海浦东新区', '硕士', '复旦大学', '离职', 'UI/前端开发工程师', '上海', 18, 28, NOW(), NOW()),
  ('demo_sk3', @demo_password, '张强', NULL, '13900000003', 'zhang.qiang@example.com', 31, '深圳南山区', '本科', '华南理工大学', '在职', '后端开发工程师', '深圳', 22, 35, NOW(), NOW()),
  ('demo_sk4', @demo_password, '赵志远', NULL, '13900000004', 'zhao.zhiyuan@example.com', 30, '北京海淀区', '硕士', '北京邮电大学', '在职', '高级前端工程师/AI架构师', '北京', 30, 45, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  email = VALUES(email),
  age = VALUES(age),
  address = VALUES(address),
  edu_back = VALUES(edu_back),
  alma_mater = VALUES(alma_mater),
  state = VALUES(state),
  ex_position = VALUES(ex_position),
  ex_city = VALUES(ex_city),
  ex_salary_min = VALUES(ex_salary_min),
  ex_salary_max = VALUES(ex_salary_max),
  update_time = NOW();

INSERT INTO job (hr_id, job_name, job_desc, requirement, keywords, salary, work_address, work_experience, status, delivery_count, create_time, update_time)
SELECT h.id, '高级前端工程师/AI产品架构', '负责 AI 招聘产品前端架构、智能简历分析与面试协同模块。', '熟悉 Vue3、TypeScript、工程化体系，有 LLM 应用落地经验优先。', 'Vue3,TypeScript,LLM应用,架构设计,性能优化', '28K-45K/月', '深圳/北京', '5年以上', 1, 3, NOW(), NOW()
FROM hr h
WHERE h.username = 'demo_hr1'
  AND NOT EXISTS (SELECT 1 FROM job j WHERE j.hr_id = h.id AND j.job_name = '高级前端工程师/AI产品架构');

INSERT INTO job (hr_id, job_name, job_desc, requirement, keywords, salary, work_address, work_experience, status, delivery_count, create_time, update_time)
SELECT h.id, 'UI/前端开发工程师', '负责 B 端招聘系统体验优化、组件库建设和候选人工作台开发。', '具备扎实前端基础，理解交互设计和复杂表单体验。', 'Vue3,Element Plus,交互设计,组件库,可视化', '18K-30K/月', '杭州', '3-5年', 1, 1, NOW(), NOW()
FROM hr h
WHERE h.username = 'demo_hr1'
  AND NOT EXISTS (SELECT 1 FROM job j WHERE j.hr_id = h.id AND j.job_name = 'UI/前端开发工程师');

INSERT INTO job (hr_id, job_name, job_desc, requirement, keywords, salary, work_address, work_experience, status, delivery_count, create_time, update_time)
SELECT h.id, '后端开发工程师', '负责投递、消息、AI 匹配等后端服务设计与性能优化。', '熟悉 Spring Boot、MySQL、Redis，具备高并发接口设计经验。', 'Spring Boot,MySQL,Redis,接口设计,AI匹配', '22K-36K/月', '上海', '3-5年', 1, 1, NOW(), NOW()
FROM hr h
WHERE h.username = 'demo_hr2'
  AND NOT EXISTS (SELECT 1 FROM job j WHERE j.hr_id = h.id AND j.job_name = '后端开发工程师');

INSERT INTO resume (seeker_id, file_name, file_url, is_parsed, parse_fail_reason, create_time, update_time)
SELECT s.id, CONCAT(s.real_name, '-演示简历.docx'), CONCAT('/upload/resume/demo/', s.username, '.docx'), 1, NULL, NOW(), NOW()
FROM seeker s
WHERE s.username IN ('demo_sk1', 'demo_sk2', 'demo_sk3', 'demo_sk4')
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name),
  file_url = VALUES(file_url),
  is_parsed = 1,
  parse_fail_reason = NULL,
  update_time = NOW();

INSERT INTO resume_parse_result (resume_id, keyword_coverage, basic_info, work_experience, skills, work_history, ai_summary, improvement_suggestions, create_time, update_time)
SELECT r.id, 88,
       JSON_OBJECT('realName', s.real_name, 'phone', s.phone, 'email', s.email, 'age', s.age, 'eduBack', s.edu_back, 'almaMater', s.alma_mater),
       CASE s.username
         WHEN 'demo_sk2' THEN '3年'
         WHEN 'demo_sk3' THEN '6年'
         ELSE '5年'
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN JSON_ARRAY('Vue 3', 'TypeScript', 'LLM应用', '组件化', '性能优化')
         WHEN 'demo_sk2' THEN JSON_ARRAY('Vue 3', 'Element Plus', 'Figma', '可视化', '交互设计')
         WHEN 'demo_sk3' THEN JSON_ARRAY('Spring Boot', 'MySQL', 'Redis', '消息队列', '高并发')
         ELSE JSON_ARRAY('Vue 3', 'TypeScript', 'AI Agent', '架构设计', '英语沟通')
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN JSON_ARRAY(JSON_OBJECT('company','字节跳动','position','高级前端工程师','startTime','2022.06','endTime','至今','description','负责 AI 协同产品的前端架构和 LLM 应用接入。','coreSkills',JSON_ARRAY('Vue 3','TypeScript','LLM应用')))
         WHEN 'demo_sk2' THEN JSON_ARRAY(JSON_OBJECT('company','网易','position','前端开发工程师','startTime','2021.07','endTime','至今','description','负责低代码页面搭建、组件库与设计系统落地。','coreSkills',JSON_ARRAY('Element Plus','可视化','交互设计')))
         WHEN 'demo_sk3' THEN JSON_ARRAY(JSON_OBJECT('company','美团','position','后端开发工程师','startTime','2020.03','endTime','至今','description','负责订单与消息服务，优化核心链路响应时间。','coreSkills',JSON_ARRAY('Spring Boot','Redis','MySQL')))
         ELSE JSON_ARRAY(JSON_OBJECT('company','腾讯','position','AI 前端架构师','startTime','2021.04','endTime','至今','description','负责大模型应用平台前端架构和跨团队技术方案推进。','coreSkills',JSON_ARRAY('AI Agent','TypeScript','架构设计')))
       END,
       '候选人与目标岗位匹配度较高，项目经历完整。',
       '建议补充更多量化结果，例如性能提升比例、业务转化率和团队协作范围。',
       NOW(), NOW()
FROM resume r
JOIN seeker s ON s.id = r.seeker_id
WHERE s.username IN ('demo_sk1', 'demo_sk2', 'demo_sk3', 'demo_sk4')
ON DUPLICATE KEY UPDATE
  keyword_coverage = VALUES(keyword_coverage),
  basic_info = VALUES(basic_info),
  work_experience = VALUES(work_experience),
  skills = VALUES(skills),
  work_history = VALUES(work_history),
  ai_summary = VALUES(ai_summary),
  improvement_suggestions = VALUES(improvement_suggestions),
  update_time = NOW();

INSERT INTO delivery (job_id, seeker_id, resume_id, status, hr_comment, delivery_time, update_time)
SELECT j.id, s.id, r.id,
       CASE s.username
         WHEN 'demo_sk1' THEN 3
         WHEN 'demo_sk2' THEN 0
         WHEN 'demo_sk3' THEN 2
         ELSE 1
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '技术栈匹配度不足，暂不推进。'
         ELSE NULL
       END,
       NOW() - INTERVAL (FIELD(s.username, 'demo_sk1','demo_sk2','demo_sk3','demo_sk4')) DAY,
       NOW()
FROM job j
JOIN hr h ON h.id = j.hr_id
JOIN seeker s ON s.username IN ('demo_sk1', 'demo_sk2', 'demo_sk3', 'demo_sk4')
JOIN resume r ON r.seeker_id = s.id
WHERE h.username = 'demo_hr1'
  AND j.job_name = '高级前端工程师/AI产品架构'
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  status = VALUES(status),
  hr_comment = VALUES(hr_comment),
  update_time = NOW();

INSERT INTO ai_match_result (delivery_id, match_score, match_level, job_keywords, resume_keywords, match_detail, core_advantages, potential_risks, skill_tags, analysis_time, update_time)
SELECT d.id,
       CASE s.username
         WHEN 'demo_sk1' THEN 92.5
         WHEN 'demo_sk2' THEN 78.0
         WHEN 'demo_sk3' THEN 64.0
         ELSE 88.0
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN '极高潜力'
         WHEN 'demo_sk2' THEN '高潜力'
         WHEN 'demo_sk3' THEN '中等潜力'
         ELSE '高潜力'
       END,
       JSON_ARRAY('Vue3','TypeScript','LLM应用','架构设计'),
       CASE s.username
         WHEN 'demo_sk3' THEN JSON_ARRAY('Spring Boot','MySQL','Redis')
         ELSE JSON_ARRAY('Vue 3','TypeScript','AI应用')
       END,
       '候选人技能与岗位关键词匹配，具备相关项目经验。',
       CASE s.username
         WHEN 'demo_sk1' THEN 'AI 应用落地经验充分，前端架构能力突出。'
         WHEN 'demo_sk2' THEN '交互意识好，组件库经验较强。'
         WHEN 'demo_sk3' THEN '后端能力扎实，但前端岗位适配度一般。'
         ELSE '架构能力、英语沟通和 AI Agent 经验突出。'
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '前端工程化经验不足。'
         ELSE '需进一步确认业务量化成果。'
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN 'LLM应用专家,架构能力强,英语流利'
         WHEN 'demo_sk2' THEN '组件库,交互设计,可视化'
         WHEN 'demo_sk3' THEN 'Spring Boot,Redis,MySQL'
         ELSE 'AI Agent,架构设计,英语流利'
       END,
       NOW(), NOW()
FROM delivery d
JOIN seeker s ON s.id = d.seeker_id
JOIN job j ON j.id = d.job_id
JOIN hr h ON h.id = j.hr_id
WHERE h.username = 'demo_hr1'
  AND j.job_name = '高级前端工程师/AI产品架构'
ON DUPLICATE KEY UPDATE
  match_score = VALUES(match_score),
  match_level = VALUES(match_level),
  job_keywords = VALUES(job_keywords),
  resume_keywords = VALUES(resume_keywords),
  match_detail = VALUES(match_detail),
  core_advantages = VALUES(core_advantages),
  potential_risks = VALUES(potential_risks),
  skill_tags = VALUES(skill_tags),
  update_time = NOW();

INSERT INTO interview_message (delivery_id, hr_id, seeker_id, interview_date, interview_time, interview_type, interview_round, interview_address, contact_name, contact_phone, remark, status, reject_reason, create_time, update_time)
SELECT d.id, h.id, s.id, '2026-05-12', '09:00:00', '线上面试', '初试', '腾讯会议：http://meeting.tencent.com/xxx', '林秋雅', '13800000000', '请提前10分钟进入会议，准备自我介绍及项目作品',
       CASE s.username
         WHEN 'demo_sk1' THEN 1
         WHEN 'demo_sk2' THEN 0
         WHEN 'demo_sk3' THEN 2
         ELSE 0
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '已接受其他offer'
         ELSE NULL
       END,
       NOW() - INTERVAL (FIELD(s.username, 'demo_sk1','demo_sk2','demo_sk3','demo_sk4')) HOUR,
       NOW()
FROM delivery d
JOIN seeker s ON s.id = d.seeker_id
JOIN job j ON j.id = d.job_id
JOIN hr h ON h.id = j.hr_id
WHERE h.username = 'demo_hr1'
  AND j.job_name = '高级前端工程师/AI产品架构'
  AND NOT EXISTS (
    SELECT 1 FROM interview_message im
    WHERE im.delivery_id = d.id
  );

UPDATE job j
JOIN (
  SELECT job_id, COUNT(*) AS cnt
  FROM delivery
  GROUP BY job_id
) d ON d.job_id = j.id
SET j.delivery_count = d.cnt;

-- 如果你已经用页面注册了 HR 测试账号 123123123，也给这个账号挂一份岗位和候选人数据。
UPDATE hr
SET real_name = COALESCE(real_name, '招聘经理'),
    company_name = COALESCE(company_name, 'AI-Hire 演示公司'),
    phone = COALESCE(phone, '13812312312'),
    email = COALESCE(email, 'hr123123123@example.com'),
    update_time = NOW()
WHERE username = '123123123';

INSERT INTO job (hr_id, job_name, job_desc, requirement, keywords, salary, work_address, work_experience, status, delivery_count, create_time, update_time)
SELECT h.id, 'AI 高级前端工程师', '负责 AI 招聘系统前端架构、候选人筛选池、消息通知与简历智能分析模块。', '熟悉 Vue3、Element Plus、复杂业务表单和 AI 应用交互，有产品意识优先。', 'Vue3,Element Plus,AI应用,智能筛选,前端架构', '25K-40K/月', '上海/远程', '3-5年', 1, 4, NOW(), NOW()
FROM hr h
WHERE h.username = '123123123'
  AND NOT EXISTS (SELECT 1 FROM job j WHERE j.hr_id = h.id AND j.job_name = 'AI 高级前端工程师');

INSERT INTO delivery (job_id, seeker_id, resume_id, status, hr_comment, delivery_time, update_time)
SELECT j.id, s.id, r.id,
       CASE s.username
         WHEN 'demo_sk1' THEN 3
         WHEN 'demo_sk2' THEN 0
         WHEN 'demo_sk3' THEN 2
         ELSE 1
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '技术方向暂不匹配，先进入人才库。'
         ELSE NULL
       END,
       NOW() - INTERVAL (FIELD(s.username, 'demo_sk1','demo_sk2','demo_sk3','demo_sk4')) DAY,
       NOW()
FROM hr h
JOIN job j ON j.hr_id = h.id AND j.job_name = 'AI 高级前端工程师'
JOIN seeker s ON s.username IN ('demo_sk1', 'demo_sk2', 'demo_sk3', 'demo_sk4')
JOIN resume r ON r.seeker_id = s.id
WHERE h.username = '123123123'
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  status = VALUES(status),
  hr_comment = VALUES(hr_comment),
  update_time = NOW();

INSERT INTO ai_match_result (delivery_id, match_score, match_level, job_keywords, resume_keywords, match_detail, core_advantages, potential_risks, skill_tags, analysis_time, update_time)
SELECT d.id,
       CASE s.username
         WHEN 'demo_sk1' THEN 91.0
         WHEN 'demo_sk2' THEN 82.0
         WHEN 'demo_sk3' THEN 66.0
         ELSE 89.0
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN '极高潜力'
         WHEN 'demo_sk2' THEN '高潜力'
         WHEN 'demo_sk3' THEN '中等潜力'
         ELSE '高潜力'
       END,
       JSON_ARRAY('Vue3','Element Plus','AI应用','前端架构'),
       CASE s.username
         WHEN 'demo_sk3' THEN JSON_ARRAY('Spring Boot','Redis','MySQL')
         ELSE JSON_ARRAY('Vue 3','TypeScript','AI应用')
       END,
       '当前候选人与 AI 高级前端岗位匹配度较高。',
       CASE s.username
         WHEN 'demo_sk1' THEN '前端架构和 LLM 应用经验充分。'
         WHEN 'demo_sk2' THEN '组件库、交互设计和 B 端体验能力较强。'
         WHEN 'demo_sk3' THEN '后端工程能力扎实，可作为跨端协作候选人。'
         ELSE 'AI Agent、架构设计和英语沟通能力突出。'
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '前端主导经验偏少。'
         ELSE '建议进一步确认量化产出。'
       END,
       CASE s.username
         WHEN 'demo_sk1' THEN 'LLM应用专家,架构能力强,英语流利'
         WHEN 'demo_sk2' THEN '组件库,交互设计,可视化'
         WHEN 'demo_sk3' THEN 'Spring Boot,Redis,MySQL'
         ELSE 'AI Agent,架构设计,英语流利'
       END,
       NOW(), NOW()
FROM delivery d
JOIN seeker s ON s.id = d.seeker_id
JOIN job j ON j.id = d.job_id
JOIN hr h ON h.id = j.hr_id
WHERE h.username = '123123123'
  AND j.job_name = 'AI 高级前端工程师'
ON DUPLICATE KEY UPDATE
  match_score = VALUES(match_score),
  match_level = VALUES(match_level),
  job_keywords = VALUES(job_keywords),
  resume_keywords = VALUES(resume_keywords),
  match_detail = VALUES(match_detail),
  core_advantages = VALUES(core_advantages),
  potential_risks = VALUES(potential_risks),
  skill_tags = VALUES(skill_tags),
  update_time = NOW();

INSERT INTO interview_message (delivery_id, hr_id, seeker_id, interview_date, interview_time, interview_type, interview_round, interview_address, contact_name, contact_phone, remark, status, reject_reason, create_time, update_time)
SELECT d.id, h.id, s.id, '2026-05-12', '09:00:00', '线上面试', '初试', '腾讯会议：http://meeting.tencent.com/xxx', '招聘经理', '13812312312', '请提前10分钟进入会议，准备自我介绍及项目作品',
       CASE s.username
         WHEN 'demo_sk1' THEN 1
         WHEN 'demo_sk2' THEN 0
         WHEN 'demo_sk3' THEN 2
         ELSE 0
       END,
       CASE s.username
         WHEN 'demo_sk3' THEN '已接受其他offer'
         ELSE NULL
       END,
       NOW() - INTERVAL (FIELD(s.username, 'demo_sk1','demo_sk2','demo_sk3','demo_sk4')) HOUR,
       NOW()
FROM delivery d
JOIN seeker s ON s.id = d.seeker_id
JOIN job j ON j.id = d.job_id
JOIN hr h ON h.id = j.hr_id
WHERE h.username = '123123123'
  AND j.job_name = 'AI 高级前端工程师'
  AND NOT EXISTS (
    SELECT 1 FROM interview_message im
    WHERE im.delivery_id = d.id
  );

UPDATE job j
JOIN (
  SELECT job_id, COUNT(*) AS cnt
  FROM delivery
  GROUP BY job_id
) d ON d.job_id = j.id
SET j.delivery_count = d.cnt;
