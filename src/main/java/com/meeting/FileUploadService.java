package com.meeting;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    // 使用项目内的静态目录
    private final String uploadPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    public String uploadImage(MultipartFile file) throws IOException {
        // 创建目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成唯一文件名
        String newFileName = UUID.randomUUID().toString() + ".jpg";
        
        // 保存文件
        File destFile = new File(uploadPath + newFileName);
        file.transferTo(destFile);
        
        // 返回访问路径（Spring Boot 会自动映射 static 目录）
        return "/uploads/" + newFileName;
    }
}