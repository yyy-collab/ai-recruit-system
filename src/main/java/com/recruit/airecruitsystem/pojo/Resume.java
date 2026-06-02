package com.recruit.airecruitsystem.pojo;

import java.util.Date;

public class Resume {
    private Integer id;
    private Integer seekerId;          // 所属求职者ID
    private String resumeFileName;   // 文件名
    private String resumeFileUrl;    // 文件存储地址
    private Integer isParsed;        // 0-未解析,1-解析成功,2-解析中,3-解析失败
    private String parsedText;       // 从简历中解析出的纯文本，用于匹配度计算
    private Date createTime;
    private Date updateTime;

    // Getter 和 Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSeekerId() { return seekerId; }
    public void setSeekerId(Integer seekerId) { this.seekerId = seekerId; }

    public String getResumeFileName() { return resumeFileName; }
    public void setResumeFileName(String resumeFileName) { this.resumeFileName = resumeFileName; }

    public String getResumeFileUrl() { return resumeFileUrl; }
    public void setResumeFileUrl(String resumeFileUrl) { this.resumeFileUrl = resumeFileUrl; }

    public Integer getIsParsed() { return isParsed; }
    public void setIsParsed(Integer isParsed) { this.isParsed = isParsed; }

    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}