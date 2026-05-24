# AI 招聘猎头系统接口文档 - V1.13

## 通用说明

1. 所有接口统一返回 `application/json` 格式数据

2. 所有接口统一使用 `application/json` 格式提交请求参数（文件上传接口除外，使用 `multipart/form-data`）

3. 基础 URL: `http://localhost:8080`

4. **角色区分机制**：系统首页先选择 "求职者" 或 "HR" 角色，再进入对应登录 / 注册页面，后端通过接口路径前缀 (`/seeker`/`/hr`) 区分用户角色

5. 响应码规则:

    - `0` - 成功
    - `10001` - 参数格式错误
    - `10002` - 用户名已存在
    - `10003` - 用户名或密码错误
    - `10004` - 邮箱格式无效
    - `10005` - JWT 令牌过期
    - `10006` - 资源不存在
    - `10007` - 操作频繁，请稍后再试
    - `10008` - 原密码错误
    - `10009` - 两次输入密码不一致
    - `10010` - 文件格式不支持
    - `10011` - 文件大小超出限制
    - `10012` - 不能重复投递同一岗位
    - `10013` - 请先完善个人信息后再进行此操作
    - `10014` - 已上传过简历，每个求职者只能上传一份
    - `10015` - 岗位已下线，无法投递
    - `10016` - 简历正在解析中，请稍后再试
    - `10017` - 简历解析失败，请重新上传
    - `10018` - 面试邀请不存在或无权限操作
    - `10019` - 面试邀请已处理，无法重复操作
    - `10020` - 拒绝原因不能为空
    - `10021` - 简历已存在投递记录，无法删除
    - `10022` - 邮箱未注册或验证码错误/过期
    - `10023` - 密码修改成功，需要重新登录
    - `10024` - HR名下还有上线岗位，无法注销

6. 登录后请求头必须携带: `Authorization: Bearer JWT令牌`

7. 未登录返回 HTTP 状态码 `401`, 越权访问返回 `403`

8. 简历文件仅支持: **doc、docx** 格式，单文件大小≤20MB，每个求职者只能上传一份简历

9. 密码规则: 8~16 位字符，必须包含字母和数字

10. 所有接口强制使用 HTTPS 协议

11. JWT 令牌有效期: 2 小时，支持刷新机制，令牌中包含用户 ID 和角色信息

    - 刷新条件：令牌过期前 30 分钟内可调用刷新接口，过期令牌无法刷新
    - 刷新限制：**24 小时内最多刷新 10 次**（超出返回错误码 10007），**过期前 30 分钟内刷新不计入次数限制**

12. 请求超时时间: 30 秒

13. 投递状态定义: `0-待处理`, `1-通过`, `2-淘汰`, `3-待面试`

14. 简历解析状态: `0-未解析`, `1-解析成功`, `2-解析中`, `3-解析失败`

15. 简历修改规则：不支持在线编辑简历内容，若解析有误或需更新简历，请删除原简历后重新上传

16. 面试邀请状态定义: `0-待确认`, `1-已接受`, `2-已拒绝`

17. 消息类型定义: `1-面试邀请`

18. 强制权限校验规则（后端硬校验，不可绕过）

- 所有需登录接口，**后端自动校验 token 中的用户 ID / 角色与资源归属关系**
- 越权访问直接返回 HTTP 状态码`403`，不返回业务错误码
- 权限校验范围：

    (1) HR：仅可操作 / 查看
    自己发布的岗位、自己岗位的投递、自己发送的面试邀请

    (2) 求职者：仅可操作 / 查看
    自己的信息、自己的简历、自己的投递、自己收到的面试邀请

19. 接口幂等性规则：

- 投递接口：基于 job_id + seeker_id 做幂等校验，重复投递返回 10012
- 简历上传接口：基于 seeker_id 做幂等校验，重复上传返回 10014

20. 通用格式校验正则：

- 手机号：`^1[3-9]\\d{9}$`
- 邮箱：`^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$`
- 用户名：`^[a-zA-Z0-9_]{5,16}$`

21. 分页参数默认规则：

- `pageNum`：默认值 1，不传则按 1 处理
- `pageSize`：默认值 10，不传则按 10 处理，最大值限制为 50

22. 求职者必须完善真实姓名、电话、邮箱后才能上传简历和投递岗位；
HR也必须完善真实姓名、公司名称、联系方式后才能发布岗位

## 1. 求职者相关接口

### 1.1 求职者注册

#### 1.1.1 基本信息

请求路径: `/seeker/register`

请求方式: `POST`

接口描述：求职者角色页面使用，仅需账号密码完成注册，其他信息登录后完善

#### 1.1.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明   | 类型   | 是否必须 | 备注                            |
| :------- | :----- | :----- | :------- | :------------------------------ |
| username | 用户名 | string | 是       | 5~16 位非空字符，全局唯一       |
| password | 密码   | string | 是       | 8~16 位字符，必须包含字母和数字 |

#### 1.1.3 请求示例

```json
{
  "username": "zhangsan123",
  "password": "Abc123456"
}
```

#### 1.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "注册成功，请登录后完善个人信息",
  "data": null
}
```

### 1.2 求职者登录

#### 1.2.1 基本信息

请求路径: `/seeker/login`

请求方式: `POST`

接口描述：求职者角色页面使用，账号密码登录

#### 1.2.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明   | 类型   | 是否必须 | 备注                            |
| :------- | :----- | :----- | :------- | :------------------------------ |
| username | 用户名 | string | 是       | 5~16 位非空字符                 |
| password | 密码   | string | 是       | 8~16 位字符，必须包含字母和数字 |

#### 1.2.3 请求示例

```json
{
  "username": "zhangsan123",
  "password": "Abc123456"
}
```

#### 1.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200,
    "is_info_complete": false
  }
}
```

#### 1.2.5 备注

- 登录成功后获取 JWT 令牌，后续所有请求需在请求头携带该令牌
- `is_info_complete`字段标识用户是否已完善基本信息，未完善时前端引导至信息填写页面

### 1.3 刷新 JWT 令牌

#### 1.3.1 基本信息

请求路径: `/seeker/refreshToken`

请求方式: `POST`

接口描述：刷新即将过期的 JWT 令牌

#### 1.3.2 请求参数

无（仅需在请求头携带原 JWT 令牌）

#### 1.3.3 响应数据样例

```json
{
  "code": 0,
  "msg": "刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200
  }
}
```

### 1.4 获取当前求职者信息

#### 1.4.1 基本信息

请求路径: `/seeker/userInfo`

请求方式: `GET`

接口描述：获取当前登录求职者的详细信息，未完善字段返回 null

#### 1.4.2 请求参数

无

#### 1.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "real_name": null,
    "avatar_url": null,
    "phone": null,
    "email": null,
    "age": null,
    "address": null,
    "edu_back": null,
    "alma_mater": null,
    "state": null,
    "ex_postion": null,
    "ex_city": null,
    "ex_salary_min": null,
    "ex_salary_max": null,
    "create_time": "2026-05-16 14:40:00",
    "update_time": "2026-05-16 14:40:00"
  }
}
```

### 1.5 更新求职者信息

#### 1.5.1 基本信息

请求路径: `/seeker/update`

请求方式: `PUT`

接口描述：登录后完善或修改个人信息，无需传入用户 ID (从 token 中获取)

#### 1.5.2 请求参数

请求参数格式: `application/json`

| 参数名称      | 说明         | 类型   | 是否必须 | 备注                       |
| :------------ | :----------- | :----- | :------- | :------------------------- |
| real_name     | 真实姓名     | string | 否       | 2~20 位字符                |
| avatar_url    | 头像 URL     | string | 否       | 公共文件上传接口返回的 URL |
| phone         | 联系电话     | string | 否       | 11 位手机号格式            |
| email         | 邮箱         | string | 否       | 合法邮箱格式，唯一         |
| age           | 年龄         | int    | 否       | 16~65 岁                   |
| address       | 居住地址     | string | 否       | -                          |
| edu_back      | 学历         | string | 否       | 专科 / 本科 / 硕士 / 博士  |
| alma_mater    | 毕业院校     | string | 否       | -                          |
| state         | 求职状态     | string | 否       | 在职 / 离职 / 应届毕业生   |
| ex_postion    | 期望职位     | string | 否       | -                          |
| ex_city       | 期望城市     | string | 否       | -                          |
| ex_salary_min | 期望最低薪资 | int    | 否       | 单位：K                    |
| ex_salary_max | 期望最高薪资 | int    | 否       | 单位：K                    |

#### 1.5.3 请求示例

```json
{
  "real_name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@163.com",
  "age": 25,
  "edu_back": "本科",
  "alma_mater": "XX大学",
  "state": "在职",
  "ex_postion": "Java开发工程师",
  "ex_city": "北京",
  "ex_salary_min": 15,
  "ex_salary_max": 25
}
```

#### 1.5.4 响应数据样例

```json
{
  "code": 0,
  "msg": "信息更新成功",
  "data": null
}
```

### 1.6 求职者修改密码

#### 1.6.1 基本信息

请求路径: `/seeker/updatePwd`

请求方式: `PATCH`

接口描述：修改当前登录求职者的密码

#### 1.6.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明       | 类型   | 是否必须 | 备注                            |
| :------- | :--------- | :----- | :------- | :------------------------------ |
| old_pwd  | 原密码     | string | 是       | 8~16 位字符                     |
| new_pwd  | 新密码     | string | 是       | 8~16 位字符，必须包含字母和数字 |
| re_pwd   | 确认新密码 | string | 是       | 需与 new_pwd 一致               |

#### 1.6.3 请求示例

```json
{
  "old_pwd": "Abc123456",
  "new_pwd": "Xyz789012",
  "re_pwd": "Xyz789012"
}
```

#### 1.6.4 响应数据样例

```json
{
  "code": 0,
  "msg": "密码修改成功",
  "data": null
}
```

### 1.7 求职者登出接口

#### 1.7.1 基本信息

请求路径: `/seeker/logout`

请求方式: `POST`

接口描述：登出当前求职者账号，使当前 JWT 令牌立即失效

#### 1.7.2 请求参数

无（请求头必须携带`Authorization: Bearer JWT令牌`）

#### 1.7.3 响应数据样例

```json
{
  "code": 0,
  "msg": "登出成功",
  "data": null
}
```

#### 1.7.4 备注

- 登出后令牌立即作废，再次使用返回`401`
- 前端需清空本地存储的 token 与用户信息

### 1.8 求职者账号注销

#### 1.8.1 基本信息

请求路径: `/seeker/delete`

请求方式: `DELETE`

接口描述：注销当前登录的求职者账号

#### 1.8.2 请求参数

| 参数名称 | 说明     | 类型   | 是否必须 | 备注     |
| :------- | :------- | :----- | :------- | :------- |
| password | 登录密码 | string | 是       | 身份验证 |

#### 1.8.3 响应数据样例

```json
{
  "code": 0,
  "msg": "账号注销成功",
  "data": null
}
```

### 1.9 求职者重置密码

#### 1.9.1 基本信息

请求路径: `/seeker/resetPwd`

请求方式: `POST`

接口描述：未登录状态下通过邮箱重置密码

#### 1.9.2 请求参数

| 参数名称 | 说明     | 类型   | 是否必须 | 备注             |
| :------- | :------- | :----- | :------- | :--------------- |
| username | 用户名   | string | 是       |                  |
| email    | 邮箱     | string | 是       | 注册时绑定的邮箱 |
| code     | 验证码   | string | 是       | 6 位数字         |
| new_pwd  | 新密码   | string | 是       | 符合密码规则     |
| re_pwd   | 确认密码 | string | 是       | 与新密码一致     |

#### 1.9.3 响应数据样例

```
{
  "code": 0,
  "msg": "密码重置成功，请重新登录",
  "data": null
}
```

## 2. HR 用户相关接口

### 2.1 HR 注册

#### 2.1.1 基本信息

请求路径: `/hr/register`

请求方式: `POST`

接口描述: HR 角色页面使用，仅需账号密码完成注册，其他信息登录后完善

#### 2.1.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明   | 类型   | 是否必须 | 备注                            |
| :------- | :----- | :----- | :------- | :------------------------------ |
| username | 用户名 | string | 是       | 5~16 位非空字符，全局唯一       |
| password | 密码   | string | 是       | 8~16 位字符，必须包含字母和数字 |

#### 2.1.3 请求示例

```json
{
  "username": "hr_li",
  "password": "Hr123456"
}
```

#### 2.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "注册成功，请登录完善企业信息",
  "data": null
}
```

### 2.2 HR 登录

#### 2.2.1 基本信息

请求路径: `/hr/login`

请求方式: `POST`

接口描述: HR 角色页面使用，账号密码登录

#### 2.2.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明   | 类型   | 是否必须 | 备注            |
| :------- | :----- | :----- | :------- | :-------------- |
| username | 用户名 | string | 是       | 5~16 位非空字符 |
| password | 密码   | string | 是       | 8~16 位字符     |

#### 2.2.3 请求示例

```json
{
  "username": "hr_li",
  "password": "Hr123456"
}
```

#### 2.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200,
    "is_info_complete": false
  }
}
```

#### 2.2.5 备注

- 登录成功后获取 JWT 令牌，后续所有请求需在请求头携带该令牌
- `is_info_complete`字段标识 HR 是否已完善企业信息，未完善时前端引导至信息填写页面

### 2.3 刷新 JWT 令牌

#### 2.3.1 基本信息

请求路径: `/hr/refreshToken`

请求方式: `POST`

接口描述：刷新即将过期的 JWT 令牌

#### 2.3.2 请求参数

无（仅需在请求头携带原 JWT 令牌）

#### 2.3.3 响应数据样例

```json
{
  "code": 0,
  "msg": "刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200
  }
}
```

### 2.4 获取当前 HR 信息

#### 2.4.1 基本信息

请求路径: `/hr/userInfo`

请求方式: `GET`

接口描述：获取当前登录 HR 的详细信息，未完善字段返回 null

#### 2.4.2 请求参数

无

#### 2.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "hr_li",
    "real_name": null,
    "avatar_url": null,
    "company_name": null,
    "email": null,
    "phone": null,
    "create_time": "2026-05-16 14:45:00",
    "update_time": "2026-05-16 14:45:00"
  }
}
```

### 2.5 更新 HR 信息

#### 2.5.1 基本信息

请求路径: `/hr/update`

请求方式: `PUT`

接口描述：登录后完善或修改个人及企业信息，无需传入 HR ID (从 token 中获取)

#### 2.5.2 请求参数

请求参数格式: `application/json`

| 参数名称     | 说明     | 类型   | 是否必须 | 备注                       |
| :----------- | :------- | :----- | :------- | :------------------------- |
| real_name    | 真实姓名 | string | 否       | 2~20 位字符                |
| avatar_url   | 头像 URL | string | 否       | 公共文件上传接口返回的 URL |
| company_name | 公司名称 | string | 否       | -                          |
| email        | 企业邮箱 | string | 否       | 合法邮箱格式，唯一         |
| phone        | 联系电话 | string | 否       | 11 位手机号格式            |

#### 2.5.3 请求示例

```json
{
  "real_name": "李经理",
  "company_name": "XX科技",
  "email": "hr@xx.com",
  "phone": "13900139000"
}
```

#### 2.5.4 响应数据样例

```json
{
  "code": 0,
  "msg": "信息更新成功",
  "data": null
}
```

### 2.6 HR 修改密码

#### 2.6.1 基本信息

请求路径: `/hr/updatePwd`

请求方式: `PATCH`

接口描述：修改当前登录 HR 的密码

#### 2.6.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明       | 类型   | 是否必须 | 备注                            |
| :------- | :--------- | :----- | :------- | :------------------------------ |
| old_pwd  | 原密码     | string | 是       | 8~16 位字符                     |
| new_pwd  | 新密码     | string | 是       | 8~16 位字符，必须包含字母和数字 |
| re_pwd   | 确认新密码 | string | 是       | 需与 new_pwd 一致               |

#### 2.6.3 请求示例

```json
{
  "old_pwd": "Hr123456",
  "new_pwd": "Hr789012",
  "re_pwd": "Hr789012"
}
```

#### 2.6.4 响应数据样例

```json
{
  "code": 0,
  "msg": "密码修改成功",
  "data": null
}
```

### 2.7 HR 招聘数据统计

#### 2.7.1 基本信息

请求路径: `/hr/statistics`

请求方式: `GET`

接口描述：获取当前 HR 发布岗位的招聘数据统计

#### 2.7.2 请求参数

无

#### 2.7.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total_jobs": 5,
    "online_jobs": 3,
    "total_deliveries": 126,
    "pending_deliveries": 45,
    "passed_deliveries": 23,
    "rejected_deliveries": 58,
    "match_score_distribution": {
      "90-100": 8,
      "80-89": 25,
      "70-79": 42,
      "60-69": 31,
      "0-59": 20
    },
    "job_delivery_ranking": [
      {
        "job_id": 1,
        "job_name": "Java开发工程师",
        "delivery_count": 48
      },
      {
        "job_id": 2,
        "job_name": "前端开发工程师",
        "delivery_count": 35
      }
    ]
  }
}
```

### 2.8 HR 登出接口

#### 2.8.1 基本信息

请求路径: `/hr/logout`

请求方式: `POST`

接口描述：登出当前 HR 账号，使当前 JWT 令牌立即失效

#### 2.8.2 请求参数

无（请求头必须携带`Authorization: Bearer JWT令牌`）

#### 2.8.3 响应数据样例

```json
{
  "code": 0,
  "msg": "登出成功",
  "data": null
}
```

#### 2.8.4 备注

- 登出后令牌立即作废，再次使用返回`401`
- 前端需清空本地存储的 token 与用户信息

### 2.9 HR 重置密码

#### 2.9.1 基本信息

请求路径: `/hr/resetPwd`

请求方式: `POST`

接口描述：未登录状态下通过邮箱重置密码

#### 2.9.2 请求参数

| 参数名称 | 说明     | 类型   | 是否必须 | 备注             |
| :------- | :------- | :----- | :------- | :--------------- |
| username | 用户名   | string | 是       |                  |
| email    | 企业邮箱 | string | 是       | 注册时绑定的邮箱 |
| code     | 验证码   | string | 是       | 6 位数字         |
| new_pwd  | 新密码   | string | 是       | 符合密码规则     |
| re_pwd   | 确认密码 | string | 是       | 与新密码一致     |

## 3. 岗位管理接口

### 3.1 HR 发布岗位

#### 3.1.1 基本信息

请求路径: `/hr/job/add`

请求方式: `POST`

接口描述: HR 发布新的招聘岗位，自动关联当前 HR ID

#### 3.1.2 请求参数

请求参数格式: `application/json`

| 参数名称        | 说明         | 类型   | 是否必须 | 备注       |
| :-------------- | :----------- | :----- | :------- | :--------- |
| job_name        | 岗位名称     | string | 是       | -          |
| job_desc        | 岗位描述     | string | 是       | -          |
| requirement     | 任职要求     | string | 是       | -          |
| keywords        | 核心关键词   | string | 是       | 逗号分隔   |
| salary          | 薪资范围     | string | 否       | 如: 15-25K |
| work_address    | 工作地点     | string | 否       | -          |
| work_experience | 工作经验要求 | string | 否       | 如: 3-5 年 |

#### 3.1.3 请求示例

```json
{
  "job_name": "Java开发工程师",
  "job_desc": "负责公司核心业务系统的开发与维护",
  "requirement": "3年以上Java开发经验，熟悉SpringBoot框架",
  "keywords": "Java,SpringBoot,MySQL,Redis",
  "salary": "15-25K",
  "work_address": "北京市朝阳区",
  "work_experience": "3-5年"
}
```

#### 3.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "岗位发布成功",
  "data": {
    "job_id": 1001
  }
}
```

#### 3.1.5 备注

- 新发布岗位默认状态为 "上线"(status=1)
- **必须完善公司名称信息后才能发布岗位**，否则返回错误码`10013`

### 3.2 HR 编辑岗位

#### 3.2.1 基本信息

请求路径: `/hr/job/update`

请求方式: `PUT`

接口描述：修改已发布的岗位信息，仅可修改自己发布的岗位

#### 3.2.2 请求参数

请求参数格式: `application/json`

| 参数名称        | 说明         | 类型   | 是否必须 | 备注       |
| :-------------- | :----------- | :----- | :------- | :--------- |
| id              | 岗位 ID      | int    | 是       | -          |
| job_name        | 岗位名称     | string | 否       | -          |
| job_desc        | 岗位描述     | string | 否       | -          |
| requirement     | 任职要求     | string | 否       | -          |
| keywords        | 核心关键词   | string | 否       | 逗号分隔   |
| salary          | 薪资范围     | string | 否       | 如: 15-25K |
| work_address    | 工作地点     | string | 否       | -          |
| work_experience | 工作经验要求 | string | 否       | 如: 3-5 年 |

#### 3.2.3 请求示例

```json
{
  "id": 1001,
  "salary": "18-28K",
  "work_experience": "2-5年"
}
```

#### 3.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "岗位更新成功",
  "data": null
}
```

### 3.3 HR 岗位上下线

#### 3.3.1 基本信息

请求路径: `/hr/job/changeStatus`

请求方式: `PATCH`

接口描述：修改岗位上线 / 下线状态，仅可操作自己发布的岗位

#### 3.3.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 类型 | 是否必须 | 备注               |
| :------- | :--- | :------- | :----------------- |
| id       | int  | 是       | 岗位 ID            |
| status   | int  | 是       | 1 - 上线，0 - 下线 |

#### 3.3.3 请求示例

```json
{
  "id": 1001,
  "status": 0
}
```

#### 3.3.4 响应数据样例

```json
{
  "code": 0,
  "msg": "状态修改成功",
  "data": null
}
```

### 3.4 HR 获取我的岗位列表

#### 3.4.1 基本信息

请求路径: `/hr/job/myList`

请求方式: `GET`

接口描述: HR 查看自己发布的所有岗位

#### 3.4.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型   | 是否必须 | 备注               |
| :------- | :----- | :------- | :----------------- |
| pageNum  | int    | 否       | 默认 1             |
| pageSize | int    | 否       | 默认 10            |
| status   | int    | 否       | 1 - 上线，0 - 下线 |
| job_name | string | 否       | 模糊搜索           |

#### 3.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 15,
    "items": [
      {
        "id": 1,
        "job_name": "Java开发工程师",
        "salary": "15-25K",
        "work_address": "北京",
        "status": 1,
        "create_time": "2026-05-10 09:00:00",
        "update_time": "2026-05-15 14:30:00",
        "delivery_count": 28
      }
    ]
  }
}
```

### 3.5 求职者岗位列表 (分页 + 筛选)

#### 3.5.1 基本信息

请求路径: `/seeker/job/list`

请求方式: `GET`

接口描述：求职者查看上线岗位列表

#### 3.5.2 请求参数

| 参数名称 | 类型   | 是否必须 | 备注                                |
| :------- | :----- | :------- | :---------------------------------- |
| pageNum  | int    | 否       | 默认 1                              |
| pageSize | int    | 否       | 默认 10                             |
| job_name | string | 否       | 模糊搜索                            |
| sort     | string | 否       | salary_desc / 时间降序 / 匹配度降序 |

#### 3.5.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 30,
    "items": [
      {
        "id": 1,
        "job_name": "Java开发工程师",
        "salary": "15-25K",
        "match_score": 88
      }
    ]
  }
}
```

### 3.6 求职者查看岗位详情

#### 3.6.1 基本信息

请求路径: `/seeker/job/detail`

请求方式: `GET`

接口描述：查看上线岗位的详细信息

#### 3.6.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型 | 是否必须 | 备注    |
| :------- | :--- | :------- | :------ |
| job_id   | int  | 是       | 岗位 ID |

#### 3.6.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "job_name": "Java开发工程师",
    "job_desc": "负责公司核心业务系统的开发与维护",
    "requirement": "3年以上Java开发经验，熟悉SpringBoot框架",
    "keywords": "Java,SpringBoot,MySQL,Redis",
    "salary": "15-25K",
    "company_name": "XX科技",
    "work_address": "北京市朝阳区",
    "work_experience": "3-5年",
    "hr_name": "李经理",
    "create_time": "2026-05-10 09:00:00"
  }
}
```

### 3.7 HR 删除岗位

#### 3.7.1 基本信息

请求路径: `/hr/job/delete`

请求方式: `DELETE`

接口描述：逻辑删除自己发布的岗位

#### 3.7.2 请求参数

| 参数名称 | 类型 | 是否必须 | 备注    |
| :------- | :--- | :------- | :------ |
| id       | int  | 是       | 岗位 ID |

#### 3.7.3 响应数据样例

```json
{
  "code": 0,
  "msg": "岗位删除成功",
  "data": null
}
```

## 4. 简历管理接口

### 4.1 简历文件上传

#### 4.1.1 基本信息

请求路径: `/resume/upload`

请求方式: `POST`

接口描述: 求职者上传单份简历文件，系统**自动触发 AI 解析**

#### 4.1.2 请求参数

请求参数格式: `multipart/form-data`

| 参数名称 | 类型 | 是否必须 | 备注                 |
| :------- | :--- | :------- | :------------------- |
| file     | file | 是       | doc/docx 格式，≤20MB |

#### 4.1.3 响应数据样例

```json
{
  "code": 0,
  "msg": "上传成功，AI正在解析简历",
  "data": {
    "resume_id": 1001,
    "resume_file_name": "张三_Java开发_3年经验.docx",
    "resume_file_url": "https://xxx.oss/resume/zhangsan.docx",
    "is_parsed": 2,
    "create_time": "2026-05-12 10:30:00"
  }
}
```

#### 4.1.4 备注

- 简历文件支持 **.doc / .docx** 格式，底层采用 **Apache POI** 解析文档文本
- 单文件大小≤20MB，每个求职者只能上传 1 份简历
- 上传后自动触发 AI 解析，**Apache POI 提取文本→HanLP 分词→TF-IDF 关键词提取**
- 若已上传过简历再次调用此接口，返回错误码`10014`
- 简历更新流程：必须先调用`/resume/delete`接口删除原有简历，再重新上传新简历
- 解析过程为异步，`is_parsed=2`表示解析中，请稍后调用获取简历详情接口
- 解析失败处理流程：
    1. 若自动解析失败 (`is_parsed=3`)，优先调用`/resume/ai/reparse`接口手动重新解析
    2. 若手动重新解析仍失败，再删除原简历后重新上传
- HR 无简历上传权限，所有简历均由求职者本人上传

### 4.2 AI 重新解析简历

#### 4.2.1 基本信息

请求路径: `/resume/ai/reparse`

请求方式: `POST`

接口描述：手动触发 AI 重新解析当前求职者唯一的简历，适用于解析失败的情况

#### 4.2.2 请求参数

请求参数格式: `application/json`

| 参数名称  | 类型 | 是否必须 | 备注    |
| :-------- | :--- | :------- | :------ |
| resume_id | int  | 是       | 简历 ID |

#### 4.2.3 请求示例

```json
{
  "resume_id": 1001
}
```

#### 4.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "已触发AI重新解析，请稍后查看结果",
  "data": {
    "resume_id": 1001,
    "is_parsed": 2
  }
}
```

#### 4.2.5 备注

- 底层采用 **Apache POI** 重新解析简历文档文本
- 解析过程为异步，解析完成后会自动更新简历数据
- 若简历正在解析中，返回错误码`10016`
- 若重新解析仍失败，请删除原简历后重新上传
- 只能重新解析当前登录求职者自己的简历

### 4.3 获取我的简历列表

#### 4.3.1 基本信息

请求路径: `/resume/myList`

请求方式: `GET`

接口描述：获取当前求职者的简历及 AI 解析状态（最多返回 1 条数据）

#### 4.3.2 请求参数

无

#### 4.3.3 响应数据样例（已上传简历）

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": [
    {
      "resume_id": 1001,
      "resume_file_name": "张三_Java开发_3年经验.docx",
      "resume_file_url": "https://xxx.oss/resume/zhangsan.docx",
      "is_parsed": 1,
      "create_time": "2026-05-12 10:30:00"
    }
  ]
}
```

#### 4.3.4 响应数据样例（未上传简历）

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": []
}
```

#### 4.3.5 备注

- 由于每个求职者只能上传 1 份简历，该接口最多返回 1 条数据
- 若求职者未上传过简历，返回空数组`[]`
- `is_parsed`字段说明：0 - 未解析，1 - 解析成功，2 - 解析中，3 - 解析失败

### 4.4 获取简历 AI 解析详情

#### 4.4.1 基本信息

请求路径: `/resume/ai/detail`

请求方式: `GET`

接口描述：获取当前求职者唯一简历的完整 AI 解析结果

#### 4.4.2 请求参数

请求参数格式: `queryString`

| 参数名称  | 类型 | 是否必须 | 备注    |
| :-------- | :--- | :------- | :------ |
| resume_id | int  | 是       | 简历 ID |

#### 4.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "resume_id": 1001,
    "resume_file_name": "张三_Java开发_3年经验.docx",
    "resume_file_url": "https://xxx.oss/resume/zhangsan.docx",
    "is_parsed": 1,
    "ai_analysis": {
      "keyword_coverage": 82,
      "basic_info": {
        "real_name": "张三",
        "phone": "13800138000",
        "email": "zhangsan@163.com",
        "age": 25,
        "edu_back": "本科",
        "alma_mater": "XX大学"
      },
      "work_experience": "3年",
      "skills": ["Java", "SpringBoot", "MySQL", "Redis"],
      "work_history": [
        {
          "company": "YY科技",
          "position": "Java开发工程师",
          "start_time": "2023-03",
          "end_time": "2026-04",
          "description": "负责电商系统后端开发",
          "core_skills": ["Java", "SpringBoot", "MySQL"]
        }
      ],
      "ai_summary": "该候选人具备3年Java开发经验，技术栈匹配度较高，有电商系统开发经验，整体竞争力中等偏上",
      "improvement_suggestions": "建议补充微服务架构相关经验，完善项目描述中的技术细节"
    },
    "create_time": "2026-05-12 10:30:00",
    "update_time": "2026-05-12 10:30:00"
  }
}
```

#### 4.4.4 备注

- 只能查看当前登录求职者自己的简历详情
- 若简历正在解析中 (`is_parsed=2`)，返回错误码`10016`
- 若简历解析失败 (`is_parsed=3`)，返回错误码`10017`
- AI 解析结果中的 `skills`、`work_experience`、`work_history` 字段将被用于岗位匹配度计算，详见 8.3 节

### 4.5 删除简历

#### 4.5.1 基本信息

请求路径: `/resume/delete`

请求方式: `DELETE`

接口描述：删除当前求职者唯一的简历，删除后可重新上传新简历

#### 4.5.2 请求参数

请求参数格式: `queryString`

| 参数名称  | 类型 | 是否必须 | 备注    |
| :-------- | :--- | :------- | :------ |
| resume_id | int  | 是       | 简历 ID |

#### 4.5.3 响应数据样例

```json
{
  "code": 0,
  "msg": "简历删除成功，您现在可以上传新的简历",
  "data": null
}
```

#### 4.5.4 详细备注

1. **权限控制**：只能删除当前登录求职者自己的简历，删除他人简历返回`403`越权错误
2. **删除后的状态变化**：
    - 删除成功后，求职者的简历状态变为 "未上传"
    - `/resume/myList`接口将返回空数组`[]`
    - 可立即调用`/resume/upload`接口上传新简历
3. **对历史数据的影响**：
    - **已投递的简历数据不会被删除**，HR 仍可查看所有历史投递记录
    - 已投递的匹配度评分和 AI 分析结果会永久保留
    - 新上传的简历不会影响任何已投递的记录
4. **错误处理**：
    - 若传入的`resume_id`不存在，返回错误码`10006`(资源不存在)
    - 若未上传过简历调用此接口，返回错误码`10006`
5. **投递关联校验**：若该简历已被任何投递记录引用，则无法删除，返回错误码 `10021`（简历已存在投递记录，无法删除）。HR 端可正常查看历史投递中的简历快照。

### 4.6 简历在线预览

#### 4.6.1 基本信息

请求路径: `/resume/preview`

请求方式: `GET`

接口描述：简历文本在线预览

#### 4.6.2 请求参数

| 参数名称  | 类型 | 是否必须 | 备注    |
| :-------- | :--- | :------- | :------ |
| resume_id | int  | 是       | 简历 ID |

#### 4.6.3 响应数据样例

```
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "preview_text": "姓名：张三\n工作经验：3年Java开发"
  }
}
```

## 5. 投递管理接口

### 5.1 求职者投递岗位

#### 5.1.1 基本信息

请求路径: `/seeker/delivery/add`

请求方式: `POST`

接口描述：求职者向指定岗位投递简历（自动使用当前求职者唯一且已解析成功的简历），投递后**同步计算 AI 匹配度**

#### 5.1.2 请求参数

| 参数名称 | 类型 | 是否必须 | 备注    |
| :------- | :--- | :------- | :------ |
| job_id   | int  | 是       | 岗位 ID |

#### 5.1.3 请求示例

```json
{
  "job_id": 1001
}
```

#### 5.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "投递成功",
  "data": {
    "delivery_id": 2001,
    "match_score": 88.5,
    "match_level": "高潜力"
  }
}
```

#### 5.1.5 备注

- **必须完善求职者相关信息后才能投递**，否则返回错误码`10013`
- **必须简历解析完成（is_parsed=1）后才能投递**，否则返回`10016`（解析中）或`10017`（解析失败）
- 同一求职者不能向同一岗位重复投递（返回`10012`）
- 岗位下线后无法投递（返回`10015`）
- 匹配度由 **HanLP + TF-IDF + 余弦相似度** 同步计算，耗时 ≤ 2 秒
- 投递后删除简历不会影响本次投递结果，HR 仍可查看该简历

### 5.2 求职者查看我的投递记录

#### 5.2.1 基本信息

请求路径: `/seeker/delivery/myList`

请求方式: `GET`

接口描述：求职者查看自己的所有投递记录及 AI 匹配结果

#### 5.2.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型 | 是否必须 | 备注                                       |
| :------- | :--- | :------- | :----------------------------------------- |
| pageNum  | int  | 否       | 默认 1                                     |
| pageSize | int  | 否       | 默认 10                                    |
| status   | int  | 否       | 0 - 待处理，1 - 通过，2 - 淘汰，3 - 待面试 |

#### 5.2.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 8,
    "items": [
      {
        "delivery_id": 2001,
        "job_id": 1,
        "job_name": "Java开发工程师",
        "company_name": "XX科技",
        "salary": "15-25K",
        "resume_file_name": "张三_Java开发_3年经验.docx",
        "match_score": 88,
        "match_level": "高潜力",
        "status": 1,
        "delivery_time": "2026-05-13 14:20:00",
        "update_time": "2026-05-14 09:30:00"
      }
    ]
  }
}
```

### 5.3 HR 查看岗位投递列表

#### 5.3.1 基本信息

请求路径: `/hr/delivery/list`

请求方式: `GET`

接口描述: HR 查看自己发布岗位的所有投递记录，支持按 AI 匹配度排序

#### 5.3.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型   | 是否必须 | 备注                                                    |
| :------- | :----- | :------- | :------------------------------------------------------ |
| job_id   | int    | 是       | 岗位 ID                                                 |
| pageNum  | int    | 否       | 默认 1                                                  |
| pageSize | int    | 否       | 默认 10                                                 |
| status   | int    | 否       | 0 - 待处理，1 - 通过，2 - 淘汰，3 - 待面试              |
| sort     | string | 否       | match_score_desc (匹配度降序), time_desc (投递时间降序) |

#### 5.3.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 28,
    "items": [
      {
        "delivery_id": 2001,
        "seeker_id": 1,
        "seeker_name": "张三",
        "resume_id": 1001,
        "resume_file_name": "张三_Java开发_3年经验.docx",
        "match_score": 88,
        "match_level": "高潜力",
        "status": 0,
        "delivery_time": "2026-05-13 14:20:00"
      }
    ]
  }
}
```

#### 5.3.4 备注

- 投递状态`3-待面试`表示已向该候选人发送面试邀请，等待对方确认
- 可通过`/hr/message/list`接口查看所有面试邀请的处理状态 

### 5.4 HR 查看投递详情

#### 5.4.1 基本信息

请求路径: `/hr/delivery/detail`

请求方式: `GET`

接口描述: HR 查看投递的详细信息、简历内容和完整 AI 匹配分析结果

#### 5.4.2 请求参数

请求参数格式: `queryString`

| 参数名称    | 类型 | 是否必须 | 备注    |
| :---------- | :--- | :------- | :------ |
| delivery_id | int  | 是       | 投递 ID |

#### 5.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "delivery_id": 2001,
    "job_id": 1,
    "job_name": "Java开发工程师",
    "seeker_info": {
      "id": 1,
      "real_name": "张三",
      "phone": "13800138000",
      "email": "zhangsan@163.com",
      "age": 25,
      "edu_back": "本科",
      "alma_mater": "XX大学"
    },
    "resume_info": {
      "resume_id": 1001,
      "resume_file_url": "https://xxx.oss/resume/zhangsan.docx",
      "keyword_coverage": 82,
      "parsed_data": {
        "work_experience": "3年",
        "skills": ["Java", "SpringBoot", "MySQL"]
      }
    },
    "match_info": {
      "match_score": 88,
      "match_level": "高潜力",
      "ai_comment": "技能匹配度高，经验符合要求",
      "core_advantages": "Java、SpringBoot、高并发",
      "potential_risks": "缺少微服务架构经验",
      "skill_tags": "Java开发,后端开发,SpringBoot",
      "analysis_time": "2026-05-13 14:20:05"
    },
    "status": 0,
    "delivery_time": "2026-05-13 14:20:00",
    "update_time": "2026-05-13 14:20:00"
  }
}
```

### 5.5 HR 修改投递状态

#### 5.5.1 基本信息

请求路径: `/hr/delivery/updateStatus`

请求方式: `PATCH`

接口描述: HR 处理简历，设置投递状态

#### 5.5.2 请求参数

请求参数格式: `application/json`

| 参数名称    | 类型   | 是否必须 | 备注                           |
| :---------- | :----- | :------- | :----------------------------- |
| delivery_id | int    | 是       | 投递 ID                        |
| status      | int    | 是       | 0 - 待处理，1 - 通过，2 - 淘汰 |
| comment     | string | 否       | HR 备注信息,淘汰时必填         |

#### 5.5.3 请求示例

```json
{
  "delivery_id": 2001,
  "status": 1,
  "comment": "技术栈匹配度高，邀请面试"
}
```

#### 5.5.4 响应数据样例

```json
{
  "code": 0,
  "msg": "状态更新成功",
  "data": null
}
```

#### 5.5.5 备注

1. 仅可修改自己发布岗位的投递记录
2. **禁止手动设置为 3 - 待面试**，该状态仅能通过发送面试邀请接口自动生成
3. 状态修改后不可回退

### 5.6 HR 批量更新投递状态

#### 5.6.1 基本信息

请求路径: `/hr/delivery/batchUpdateStatus`

请求方式: `PATCH`

接口描述：批量修改投递状态

#### 5.6.2 请求参数

| 参数名称      | 类型   | 是否必须 | 备注                        |
| :------------ | :----- | :------- | :-------------------------- |
| delivery_ids  | array  | 是       | 投递 ID 列表（int数组）     |
| status        | int    | 是       | 0/1/2（不可设为3）          |
| reject_reason | string | 否       | status=2 时必填（统一原因） |

#### 5.6.3 请求示例

```json
{
  "delivery_ids": [2001, 2002],
  "status": 2,
  "reject_reason": "学历不匹配"
}
```

#### 5.6.4 响应数据样例

```json
{
  "code": 0,
  "msg": "批量更新成功",
  "data": {"success_ids": [2001, 2002]}
}
```

#### 5.6.5 备注
- 仅可批量修改自己发布岗位的投递记录，后端会逐一校验权限
- 移除 `job_id` 参数，避免冗余和不一致

## 6. 公共文件上传接口

### 6.1 公共文件上传（头像 / 非简历文件）

#### 6.1.1 基本信息

请求路径: `/common/file/upload`

请求方式: `POST`

接口描述：上传头像等非简历类文件，返回文件 URL（支持求职者 / HR 调用）

#### 6.1.2 请求参数

请求参数格式: `multipart/form-data`

| 参数名称 | 说明     | 类型   | 是否必须 | 备注                                    |
| -------- | -------- | ------ | -------- | --------------------------------------- |
| file     | 文件     | file   | 是       | 支持 jpg/png/jpeg/webp 格式，单文件≤5MB |
| type     | 文件类型 | string | 是       | avatar - 头像                           |

#### 6.1.3 响应数据样例

```json
{
  "code": 0,
  "msg": "文件上传成功",
  "data": {
    "file_url": "https://xxx.oss/avatar/zhangsan_123456.jpg",
    "file_name": "zhangsan_avatar.jpg"
  }
}
```

#### 6.1.4 备注

- 仅支持指定格式文件，非指定格式返回 10010
- 文件大小超出限制返回 10011
- 登录后才可调用，未登录返回 401

### 6.2 获取邮箱验证码

#### 6.2.1 基本信息

请求路径: `/common/verify/code`

请求方式: `POST`

接口描述：未登录状态下获取邮箱验证码（用于重置密码）

#### 6.2.2 请求参数

请求参数格式: `application/json`

| 参数名称 | 说明       | 类型   | 是否必须 | 备注                                                |
| -------- | ---------- | ------ | -------- | --------------------------------------------------- |
| email    | 邮箱       | string | 是       | 符合通用邮箱正则规则                                |
| type     | 验证码类型 | string | 是       | seeker_reset - 求职者重置密码；hr_reset-HR 重置密码 |

#### 6.2.3 请求示例

```json
{
  "email": "zhangsan@163.com",
  "type": "seeker_reset"
}
```

#### 6.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "验证码已发送至邮箱，5分钟内有效",
  "data": null
}
```

#### 6.2.5 备注

- 邮箱格式错误返回 10004
- 操作频繁（1 分钟内重复获取）返回 10007
- **根据 `type` 校验邮箱是否已被对应用户绑定**：若 `type=seeker_reset` 且邮箱未注册求职者，返回 `10022`（邮箱未注册）；若 `type=hr_reset` 且邮箱未注册 HR，同样返回 `10022`
- 验证码有效期 5 分钟，过期返回 `10022`

## 7. 消息通知与面试邀请接口

### 7.1 HR 发送面试邀请

#### 7.1.1 基本信息

请求路径: `/hr/interview/send`

请求方式: `POST`

接口描述: HR 向指定求职者发送面试邀请，自动生成消息通知

#### 7.1.2 请求参数

请求参数格式: `application/json`

| 参数名称          | 说明   | 类型 | 是否必须                                 | 备注 |
| :---------------- | :----- | :--- | :--------------------------------------- | :--- |
| delivery_id       | int    | 是   | 投递记录 ID                              |      |
| interview_date    | string | 是   | 面试日期，格式: YYYY-MM-DD               |      |
| interview_time    | string | 是   | 面试时间，格式: HH:mm                    |      |
| interview_type    | string | 是   | 面试形式：线上面试 / 现场面试 / 电话面试 |      |
| interview_round   | string | 是   | 面试轮次：初试 / 复试 / 终试             |      |
| interview_address | string | 是   | 面试地点 / 会议链接                      |      |
| contact_name      | string | 是   | 联系人姓名                               |      |
| contact_phone     | string | 是   | 联系人电话                               |      |
| remark            | string | 否   | 备注信息                                 |      |

#### 7.1.3 请求示例

```json
{
  "delivery_id": 2001,
  "interview_date": "2026-05-12",
  "interview_time": "09:00",
  "interview_type": "线上面试",
  "interview_round": "初试",
  "interview_address": "腾讯会议: http://meeting.tencent.com/xxx",
  "contact_name": "林秋雅",
  "contact_phone": "13800000000",
  "remark": "请提前10分钟进入会议，准备自我介绍及项目作品"
}
```

#### 7.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "面试邀请发送成功",
  "data": {
    "message_id": 3001
  }
}
```

#### 7.1.5 备注

- 只能向自己发布岗位的已投递求职者发送邀请
- 发送成功后，求职者端会立即收到消息通知
- 同时会自动将投递记录的状态更新为 "3 - 待面试"

### 7.2 HR 获取消息通知列表

#### 7.2.1 基本信息

请求路径: `/hr/message/list`

请求方式: `GET`

接口描述: HR 获取自己发送的所有面试邀请消息列表

#### 7.2.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型 | 是否必须 | 备注                               |
| :------- | :--- | :------- | :--------------------------------- |
| pageNum  | int  | 否       | 默认 1                             |
| pageSize | int  | 否       | 默认 10                            |
| status   | int  | 否       | 0 - 待确认，1 - 已接受，2 - 已拒绝 |

#### 7.2.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 3,
    "items": [
      {
        "message_id": 3001,
        "seeker_name": "陈志远",
        "job_name": "高级前端工程师/AI产品架构",
        "status": 1,
        "status_text": "已接受",
        "interview_date": "2026-05-12",
        "interview_time": "09:00",
        "create_time": "2026-05-10 12:00:00",
        "update_time": "2026-05-10 12:30:00"
      },
      {
        "message_id": 3002,
        "seeker_name": "李雪莹",
        "job_name": "UI/前端开发工程师",
        "status": 0,
        "status_text": "待确认",
        "interview_date": "2026-05-10",
        "interview_time": "12:00",
        "create_time": "2026-05-10 12:00:00",
        "update_time": "2026-05-10 12:00:00"
      },
      {
        "message_id": 3003,
        "seeker_name": "张强",
        "job_name": "后端开发工程师",
        "status": 2,
        "status_text": "已拒绝",
        "reject_reason": "已接受其他offer",
        "interview_date": "2026-05-10",
        "interview_time": "12:00",
        "create_time": "2026-05-10 12:00:00",
        "update_time": "2026-05-10 12:15:00"
      }
    ]
  }
}
```

#### 7.2.4 备注

- 不传入 status 参数时，返回所有状态的消息
- 消息按创建时间倒序排列
- 已拒绝的消息会显示拒绝原因

### 7.3 HR 获取面试邀请详情

#### 7.3.1 基本信息

请求路径: `/hr/message/detail`

请求方式: `GET`

接口描述: HR 获取指定面试邀请的详细信息

#### 7.3.2 请求参数

请求参数格式: `queryString`

| 参数名称   | 类型 | 是否必须 | 备注    |
| :--------- | :--- | :------- | :------ |
| message_id | int  | 是       | 消息 ID |

#### 7.3.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "message_id": 3001,
    "seeker_info": {
      "id": 1,
      "real_name": "陈志远",
      "phone": "13800138000",
      "email": "zhangsan@163.com"
    },
    "job_info": {
      "id": 1,
      "job_name": "高级前端工程师/AI产品架构"
    },
    "interview_info": {
      "interview_date": "2026-05-12",
      "interview_time": "09:00",
      "interview_type": "线上面试",
      "interview_round": "初试",
      "interview_address": "腾讯会议: http://meeting.tencent.com/xxx",
      "contact_name": "林秋雅",
      "contact_phone": "13800000000",
      "remark": "请提前10分钟进入会议，准备自我介绍及项目作品"
    },
    "status": 1,
    "status_text": "已接受",
    "reject_reason": null,
    "create_time": "2026-05-10 12:00:00",
    "update_time": "2026-05-10 12:30:00"
  }
}
```

#### 7.3.4 备注

- 只能查看自己发送的面试邀请详情
- 若消息不存在或无权限，返回错误码`10018`

### 7.4 求职者获取消息通知列表

#### 7.4.1 基本信息

请求路径: `/seeker/message/list`

请求方式: `GET`

接口描述：求职者获取自己收到的所有面试邀请消息列表

#### 7.4.2 请求参数

请求参数格式: `queryString`

| 参数名称 | 类型 | 是否必须 | 备注                               |
| :------- | :--- | :------- | :--------------------------------- |
| pageNum  | int  | 否       | 默认 1                             |
| pageSize | int  | 否       | 默认 10                            |
| status   | int  | 否       | 0 - 待确认，1 - 已接受，2 - 已拒绝 |

#### 7.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "total": 2,
    "items": [
      {
        "message_id": 3001,
        "hr_name": "李经理",
        "company_name": "XX科技",
        "job_name": "高级前端工程师/AI产品架构",
        "status": 1,
        "status_text": "已接受",
        "interview_date": "2026-05-12",
        "interview_time": "09:00",
        "create_time": "2026-05-10 12:00:00",
        "update_time": "2026-05-10 12:30:00"
      },
      {
        "message_id": 3002,
        "hr_name": "王经理",
        "company_name": "YY科技",
        "job_name": "前端开发工程师",
        "status": 0,
        "status_text": "待确认",
        "interview_date": "2026-05-11",
        "interview_time": "14:00",
        "create_time": "2026-05-10 13:00:00",
        "update_time": "2026-05-10 13:00:00"
      }
    ]
  }
}
```

#### 7.4.4 备注

- 不传入 status 参数时，返回所有状态的消息
- 消息按创建时间倒序排列

### 7.5 求职者获取面试邀请详情

#### 7.5.1 基本信息

请求路径: `/seeker/message/detail`

请求方式: `GET`

接口描述：求职者获取指定面试邀请的详细信息

#### 7.5.2 请求参数

请求参数格式: `queryString`

| 参数名称   | 类型 | 是否必须 | 备注    |
| :--------- | :--- | :------- | :------ |
| message_id | int  | 是       | 消息 ID |

#### 7.5.3 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "message_id": 3001,
    "hr_info": {
      "real_name": "李经理",
      "company_name": "XX科技",
      "contact_name": "林秋雅",
      "contact_phone": "13800000000"
    },
    "job_info": {
      "id": 1,
      "job_name": "高级前端工程师/AI产品架构"
    },
    "interview_info": {
      "interview_date": "2026-05-12",
      "interview_time": "09:00",
      "interview_type": "线上面试",
      "interview_round": "初试",
      "interview_address": "腾讯会议: http://meeting.tencent.com/xxx",
      "remark": "请提前10分钟进入会议，准备自我介绍及项目作品"
    },
    "status": 0,
    "status_text": "待确认",
    "create_time": "2026-05-10 12:00:00",
    "update_time": "2026-05-10 12:00:00"
  }
}
```

#### 7.5.4 备注

- 只能查看自己收到的面试邀请详情
- 若消息不存在或无权限，返回错误码`10018`

### 7.6 求职者处理面试邀请

#### 7.6.1 基本信息

请求路径: `/seeker/interview/handle`

请求方式: `POST`

接口描述：求职者接受或拒绝面试邀请

#### 7.6.2 请求参数

请求参数格式: `application/json`

| 参数名称      | 说明   | 类型 | 是否必须                      | 备注 |
| :------------ | :----- | :--- | :---------------------------- | :--- |
| message_id    | int    | 是   | 消息 ID                       |      |
| status        | int    | 是   | 1 - 接受，2 - 拒绝            |      |
| reject_reason | string | 否   | 拒绝原因，status=2 时必须填写 |      |

#### 7.6.3 请求示例（接受邀请）

```json
{
  "message_id": 3001,
  "status": 1
}
```

#### 7.6.4 请求示例（拒绝邀请）

```json
{
  "message_id": 3003,
  "status": 2,
  "reject_reason": "已接受其他公司offer"
}
```

#### 7.6.5 响应数据样例

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": null
}
```

#### 7.6.6 备注

- 只能处理自己收到的面试邀请
- 已处理的邀请无法重复操作，返回错误码`10019`
- 拒绝邀请时必须填写拒绝原因，否则返回错误码`10020`
- 处理结果会实时同步到 HR 端的消息列表

## 8. AI 核心能力接口

### 通用说明

本模块技术栈：**Apache POI（简历解析）+ HanLP（中文分词）+ TF-IDF（关键词提取）+ 余弦相似度（匹配度计算）**

所有接口请求头必须携带：`Authorization: Bearer JWT令牌`

### 8.1 AI 中文分词（HanLP 实现）

#### 8.1.1 基本信息

请求路径: `/ai/word/segment`

请求方式: `POST`

接口描述：使用**HanLP**对文本进行中文分词，为关键词提取提供基础能力

#### 8.1.2 请求参数

| 参数名称 | 说明       | 类型   | 是否必须 | 备注           |
| -------- | ---------- | ------ | -------- | -------------- |
| text     | 待分词文本 | string | 是       | 长度≤5000 字符 |

#### 8.1.3 请求示例

```json
{
  "text": "3年Java开发经验，熟悉SpringBoot、MySQL、Redis开发"
}
```

#### 8.1.4 响应数据样例

```json
{
  "code": 0,
  "msg": "HanLP分词成功",
  "data": {
    "words": ["3年", "Java", "开发", "经验", "SpringBoot", "MySQL", "Redis"]
  }
}
```

### 8.2 AI 关键词提取（HanLP + TF-IDF 算法）

#### 8.2.1 基本信息

请求路径: `/ai/keyword/extract`

请求方式: `POST`

接口描述：基于**HanLP 分词 + TF-IDF 算法**提取岗位 / 简历 / 文本核心关键词

#### 8.2.2 请求参数

| 参数名称  | 说明       | 类型   | 是否必须 | 备注                                        |
| --------- | ---------- | ------ | -------- | ------------------------------------------- |
| type      | 提取类型   | string | 是       | text - 普通文本；resume - 简历；job - 岗位  |
| content   | 文本内容   | string | 否       | type=text 时必填                            |
| resume_id | 简历 ID    | int    | 否       | type=resume 时必填（Apache POI 已解析简历） |
| job_id    | 岗位 ID    | int    | 否       | type=job 时必填                             |
| limit     | 关键词数量 | int    | 否       | 默认 10，最大 20                            |

#### 8.2.3 请求示例

```json
{
  "type": "resume",
  "resume_id": 1001,
  "limit": 10
}
```

#### 8.2.4 响应数据样例

```json
{
  "code": 0,
  "msg": "TF-IDF关键词提取成功",
  "data": {
    "keywords": ["Java", "SpringBoot", "MySQL", "Redis", "后端开发", "3年经验"]
  }
}
```

### 8.3 AI 岗位 - 简历匹配度计算（余弦相似度）

#### 8.3.1 基本信息

请求路径: `/ai/match/calculate`

请求方式: `POST`

接口描述：系统核心匹配引擎，基于**TF-IDF 关键词向量 + 余弦相似度**计算匹配度

#### 8.3.2 请求参数

| 参数名称  | 说明    | 类型 | 是否必须 | 备注                             |
| --------- | ------- | ---- | -------- | -------------------------------- |
| job_id    | 岗位 ID | int  | 是       | 必须为上线状态岗位               |
| resume_id | 简历 ID | int  | 是       | 必须为 Apache POI 解析成功的简历 |

#### 8.3.3 请求示例

```json
{
  "job_id": 1001,
  "resume_id": 1001
}
```

#### 8.3.4 响应数据样例

```json
{
  "code": 0,
  "msg": "余弦相似度匹配计算成功",
  "data": {
    "match_score": 88.5,
    "match_level": "高潜力",
    "algorithm": "HanLP + TF-IDF + 余弦相似度",
    "job_keywords": ["Java", "SpringBoot", "MySQL"],
    "resume_keywords": ["Java", "SpringBoot", "Redis"],
    "match_detail": "核心技术栈匹配度高，工作经验符合岗位要求"
  }
}
```

#### 8.3.5 匹配计算详情

**输入数据来源**
- 岗位数据：`job_name` + `job_desc` + `requirement` + `keywords`
- 简历数据：AI 解析结果中的以下字段  
  `skills`、`work_experience`、`work_history.position`、`work_history.core_skills`

**计算步骤**
1. **文本合并**：将上述来源的文本拼接成一个字符串
2. **分词**：使用 HanLP 标准分词，去除停用词（如“的”、“了”、“是”等）
3. **向量化**：采用 TF-IDF 算法计算每个词的权重，生成多维向量
4. **余弦相似度**：计算岗位向量与简历向量的夹角余弦值，并归一化到 0~100 分  
   \[
   \text{baseScore} = \frac{\sum (A_i \times B_i)}{\sqrt{\sum A_i^2} \times \sqrt{\sum B_i^2}} \times 100
   \]
5. **微调因子**（加分项，上限 10 分）：
   - 学历匹配：岗位隐含的学历要求（从 `requirement` 中识别）与简历 `edu_back` 一致 → +3 分
   - 工作年限匹配：从岗位 `work_experience` 中提取要求年限，与简历 `work_experience` 对比，每匹配 1 年 +1 分，最多 +7 分
6. **最终分数** = `min(baseScore + 微调分, 100)`

**匹配等级映射**
| 分数区间 | 匹配等级 |
| -------- | -------- |
| 90~100   | 极高潜力 |
| 75~89    | 高潜力   |
| 60~74    | 中等潜力 |
| 0~59     | 低潜力   |

#### 8.3.6 备注

- 完整计算流程：Apache POI 提取简历文本 → HanLP 分词 → TF-IDF 生成关键词向量 → 余弦相似度 → 微调
- 求职者投递岗位时，系统**自动同步调用此接口**并保存匹配结果
- **主动重算接口**：`/ai/match/recalculate`（POST，参数 `delivery_id`，重新计算并更新匹配度）

### 8.4 AI 重新计算投递匹配度

#### 8.4.1 基本信息

请求路径: `/ai/match/recalculate`

请求方式: `POST`

接口描述：HR 或系统管理员手动重新计算某条投递的匹配度（如岗位要求更新后）

#### 8.4.2 请求参数

| 参数名称    | 类型 | 是否必须 | 备注    |
| :---------- | :--- | :------- | :------ |
| delivery_id | int  | 是       | 投递 ID |

#### 8.4.3 响应数据样例

```json
{
  "code": 0,
  "msg": "重新计算成功",
  "data": {
    "match_score": 92.0,
    "match_level": "非常高潜力"
  }
}
```

#### 8.4.4 备注

- 仅 HR 可调用（操作自己岗位的投递）
- 重新计算后更新 `ai_match_result` 表，并更新投递记录的 `match_score`



## 9.通用技术

### 后端技术

| 技术            | 版本            | 用途                   | 备注                       |
| :-------------- | :-------------- | :--------------------- | :------------------------- |
| JDK             | 11 或 17        | Java 运行环境          | 推荐 17                    |
| Spring Boot     | 4.0.6           | 后端主框架             | 快速构建 Web 应用          |
| MyBatis         | 4.0.1 (starter) | 持久层框架             | 使用 XML 写 SQL            |
| PageHelper      | 1.4.7           | 分页插件               | 简化分页查询               |
| JJWT            | 0.11.5          | 生成/解析 JWT 令牌     | 用于登录认证               |
| jBCrypt         | 0.4             | 密码加密               | 加密存储用户密码           |
| Apache POI      | 5.2.5           | 解析 .doc / .docx 简历 | 提取简历文本内容           |
| HanLP           | portable-1.8.4  | 中文分词 & TF-IDF      | 用于关键词提取、匹配度计算 |
| Lombok          | 1.18.24         | 简化实体类代码         | 自动生成 getter/setter     |
| MySQL Connector | 8.0.33          | 连接 MySQL 数据库      | JDBC 驱动                  |

------

### 前端技术

| 技术         | 版本 | 用途                  | 备注                    |
| :----------- | :--- | :-------------------- | :---------------------- |
| Node.js      | 16+  | JavaScript 运行环境   | 推荐 18 LTS             |
| Vite         | 4.x  | 构建工具 & 开发服务器 | 比 Webpack 快           |
| Vue 3        | 3.2+ | 前端框架              | 组合式 API              |
| Vue Router   | 4.x  | 路由管理              | 实现页面跳转            |
| Pinia        | 2.x  | 状态管理              | 替代 Vuex，存储用户信息 |
| Element Plus | 2.3+ | UI 组件库             | 快速搭建后台页面        |
| Axios        | 1.4+ | HTTP 请求库           | 封装后端 API 调用       |