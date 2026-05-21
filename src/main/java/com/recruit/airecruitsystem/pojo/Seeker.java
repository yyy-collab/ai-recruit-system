package com.recruit.airecruitsystem.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seeker {

    /**
     * 求职者ID，主键自增
     */
    @NotNull(groups = {Update.class})   // 更新操作时 ID 不能为空
    private Integer id;

    /**
     * 用户名
     * 规则：5~16位字母、数字、下划线，全局唯一
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 5, max = 16, message = "用户名长度必须为5~16位")
    @Pattern(regexp = "^[a-zA-Z0-9_]{5,16}$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 密码（加密存储）
     * 规则：8~16位字符，必须包含字母和数字
     * 注意：@JsonProperty(access = WRITE_ONLY) 使得该字段只写入（接收请求参数），
     *       不会出现在响应 JSON 中，防止密码泄露。
     */
    @Getter(onMethod_ = @JsonProperty(access = JsonProperty.Access.WRITE_ONLY))
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$",
            message = "密码必须为8~16位，且同时包含字母和数字")
    private String password;

    /**
     * 真实姓名
     * 规则：2~20位字符
     */
    @Size(min = 2, max = 20, message = "真实姓名长度必须为2~20字符")
    private String realName;

    /**
     * 头像 URL
     * 由公共文件上传接口返回的地址
     */
    @Pattern(regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", message = "头像URL格式不正确")
    private String avatarUrl;

    /**
     * 手机号
     * 规则：11位数字，以1开头，第2位3-9（简单校验）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     * 规则：合法邮箱格式，全局唯一
     */
    @NotBlank(message = "邮箱不能为空")   //这里设为非空是为了注册后完善信息
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 年龄
     * 规则：16~65岁
     */
    @Min(value = 16, message = "年龄最小为16岁")
    @Max(value = 65, message = "年龄最大为65岁")
    private Integer age;

    /**
     * 居住地址
     */
    private String address;

    /**
     * 学历
     * 可选值：专科、本科、硕士、博士
     */
    @Pattern(regexp = "^(专科|本科|硕士|博士)$", message = "学历只能是：专科、本科、硕士、博士")
    private String eduBack;

    /**
     * 毕业院校
     */
    private String almaMater;

    /**
     * 求职状态
     * 可选值：在职、离职、应届毕业生
     */
    @Pattern(regexp = "^(在职|离职|应届毕业生)$", message = "求职状态只能是：在职、离职、应届毕业生")
    private String state;

    /**
     * 期望职位
     */
    private String exPosition;

    /**
     * 期望城市
     */
    private String exCity;

    /**
     * 期望最低薪资（单位：K）
     */
    @Min(value = 0, message = "期望最低薪资不能小于0")
    private Integer exSalaryMin;

    /**
     * 期望最高薪资（单位：K）
     */
    @Min(value = 0, message = "期望最高薪资不能小于0")
    private Integer exSalaryMax;

    /**
     * 当日刷新JWT次数（24h内）
     */
    @Builder.Default   // 使用 @Builder 时该字段默认值为 0
    private Integer refreshCount = 0;

    /**
     * 上次刷新JWT的时间
     * 用于判断24小时窗口和30分钟宽容期
     */
    private LocalDateTime lastRefreshTime;

    /**
     * 注册时间
     * 由数据库自动填充，插入时无需手动设置
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 由数据库自动更新
     */
    private LocalDateTime updateTime;

    /**
     * 验证分组：用于更新操作（例如更新求职者信息时要求 id 不为空）
     * 具体使用时在 Controller 方法上添加 @Validated(Update.class)
     */
    public interface Update {}
}
