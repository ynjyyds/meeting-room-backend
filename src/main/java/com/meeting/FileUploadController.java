package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    // 上传会议室图片
    @PostMapping("/room-image/{roomId}")
    public Map<String, Object> uploadRoomImage(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查会议室是否存在
            MeetingRoom room = meetingRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return response;
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "只能上传图片文件");
                return response;
            }
            
            // 上传图片
            String imageUrl = fileUploadService.uploadImage(file);
            
            // 更新会议室图片URL
            room.setImageUrl(imageUrl);
            meetingRoomRepository.save(room);
            
            response.put("success", true);
            response.put("message", "图片上传成功");
            response.put("imageUrl", imageUrl);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "上传失败：" + e.getMessage());
        }
        
        return response;
    }
}