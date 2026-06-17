package com.recruit.airecruitsystem.constant;

/**
 * 响应码规则（参照接口文档 V1.13）
 */
public class ResultCode {
    public static final int SUCCESS = 0;            // 成功
    public static final int PARAM_ERROR = 10001;     // 参数格式错误
    public static final int USERNAME_EXIST = 10002;  // 用户名已存在
    public static final int LOGIN_ERROR = 10003;     // 用户名或密码错误
    public static final int EMAIL_INVALID = 10004;   // 邮箱格式无效
    public static final int TOKEN_EXPIRED = 10005;   // JWT 令牌过期
    public static final int NOT_FOUND = 10006;       // 资源不存在
    public static final int TOO_FREQUENT = 10007;    // 操作频繁，请稍后再试
    public static final int OLD_PWD_ERROR = 10008;   // 原密码错误
    public static final int PWD_NOT_MATCH = 10009;   // 两次输入密码不一致
    public static final int FILE_FORMAT_ERROR = 10010;   // 文件格式不支持
    public static final int FILE_TOO_LARGE = 10011;      // 文件大小超出限制
    public static final int DUPLICATE_DELIVERY = 10012;  // 不能重复投递同一岗位
    public static final int INFO_INCOMPLETE = 10013;     // 请先完善个人信息后再进行此操作
    public static final int RESUME_EXIST = 10014;        // 已上传过简历，每个求职者只能上传一份
    public static final int JOB_OFFLINE = 10015;         // 岗位已下线，无法投递
    public static final int RESUME_PARSING = 10016;      // 简历正在解析中，请稍后再试
    public static final int RESUME_PARSE_FAIL = 10017;   // 简历解析失败，请重新上传
    public static final int INTERVIEW_NOT_EXIST = 10018; // 面试邀请不存在或无权限操作
    public static final int INTERVIEW_ALREADY_HANDLED = 10019; // 面试邀请已处理，无法重复操作
    public static final int REJECT_REASON_EMPTY = 10020;      // 拒绝原因不能为空
    public static final int RESUME_HAS_DELIVERY = 10021;      // 简历已存在投递记录，无法删除
    public static final int EMAIL_NOT_REGISTERED = 10022;     // 邮箱未注册或验证码错误/过期
    public static final int NEED_RELOGIN = 10023;   // 密码修改成功，需要重新登录
    public static final int HR_HAS_ONLINE_JOBS = 10024; // HR名下还有上线岗位，无法注销
    public static final int RESUME_UPDATE_BLOCKED = 10025; // 瀛樺湪寰呭鐞嗘姇閫掞紝鏆備笉鍏佽鏇存柊绠€鍘?
}
