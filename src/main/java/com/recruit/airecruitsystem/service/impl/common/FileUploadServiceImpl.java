package com.recruit.airecruitsystem.service.impl.common;

import com.recruit.airecruitsystem.service.common.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.access-path}")
    private String accessPath;

    // 允许的图片格式
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadFile(MultipartFile file, String type) throws Exception {
        // 1. 校验参数
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (!"avatar".equals(type)) {
            throw new IllegalArgumentException("无效的文件类型");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }

        // 3. 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件格式不正确");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("只支持 jpg, jpeg, png, webp 格式");
        }

        // 4. 生成存储目录：按日期分类，例如 2025/05/23/
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, dateDir);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 5. 生成唯一文件名：UUID + 时间戳 + 扩展名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis() + "." + extension;
        Path targetFile = targetDir.resolve(newFileName);

        // 6. 保存文件
        try {
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new Exception("文件保存失败", e);
        }

        // 7. 返回可访问的URL（相对路径）
        return accessPath + dateDir + "/" + newFileName;
    }
}
