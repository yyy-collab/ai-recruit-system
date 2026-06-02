package com.recruit.airecruitsystem.service.common;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 上传文件（头像）
     *
     * @param file 文件
     * @param type 文件类型（avatar）
     * @return 可访问的文件URL
     * @throws Exception 上传失败异常
     */
    String uploadFile(MultipartFile file, String type) throws Exception;
}
