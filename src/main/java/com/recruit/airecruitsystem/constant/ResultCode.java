package com.recruit.airecruitsystem.constant;

public class ResultCode {
    // 定义接口文档中的所有错误码常量，方便代码中引用，避免魔法数字
    public static final int SUCCESS = 0;
    public static final int PARAM_ERROR = 10001;
    public static final int USERNAME_EXIST = 10002;
    public static final int LOGIN_ERROR = 10003;
    public static final int EMAIL_INVALID = 10004;
    public static final int TOKEN_EXPIRED = 10005;
    public static final int NOT_FOUND = 10006;
    public static final int TOO_FREQUENT = 10007;
    public static final int OLD_PWD_ERROR = 10008;
    public static final int PWD_NOT_MATCH = 10009;
    public static final int FILE_FORMAT_ERROR = 10010;
    public static final int FILE_TOO_LARGE = 10011;
    public static final int DUPLICATE_DELIVERY = 10012;
    public static final int INFO_INCOMPLETE = 10013;
    public static final int RESUME_EXIST = 10014;
    public static final int JOB_OFFLINE = 10015;
    public static final int RESUME_PARSING = 10016;
    public static final int RESUME_PARSE_FAIL = 10017;
    public static final int INTERVIEW_NOT_EXIST = 10018;
    public static final int INTERVIEW_ALREADY_HANDLED = 10019;
    public static final int REJECT_REASON_EMPTY = 10020;
    public static final int RESUME_HAS_DELIVERY = 10021;
    public static final int EMAIL_NOT_REGISTERED = 10022;
}