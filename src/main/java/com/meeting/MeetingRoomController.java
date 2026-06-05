package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class MeetingRoomController {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    // 获取所有会议室
    @GetMapping
    public List<MeetingRoom> getAllRooms() {
        return meetingRoomRepository.findAll();
    }

    // 获取所有可用的会议室
    @GetMapping("/available")
    public List<MeetingRoom> getAvailableRooms() {
        return meetingRoomRepository.findByStatus(1);
    }

    // 根据 ID 获取单个会议室
    @GetMapping("/{id}")
    public MeetingRoom getRoomById(@PathVariable Long id) {
        return meetingRoomRepository.findById(id).orElse(null);
    }

    // ==================== 管理员功能 ====================
    
    // 添加会议室
    @PostMapping("/admin/add")
    public Map<String, Object> addRoom(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String name = body.get("name");
            Integer capacity = Integer.parseInt(body.get("capacity"));
            String location = body.get("location");
            String facilities = body.get("facilities");
            
            MeetingRoom room = new MeetingRoom();
            room.setName(name);
            room.setCapacity(capacity);
            room.setLocation(location);
            room.setFacilities(facilities);
            room.setStatus(1);
            
            meetingRoomRepository.save(room);
            
            response.put("success", true);
            response.put("message", "添加成功");
            response.put("roomId", room.getId());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加失败：" + e.getMessage());
        }
        return response;
    }
    
    // 修改会议室状态（启用/停用）
    @PostMapping("/admin/toggle")
    public Map<String, Object> toggleRoomStatus(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long roomId = Long.parseLong(body.get("roomId"));
            Integer status = Integer.parseInt(body.get("status"));
            
            MeetingRoom room = meetingRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return response;
            }
            
            room.setStatus(status);
            meetingRoomRepository.save(room);
            
            response.put("success", true);
            response.put("message", status == 1 ? "已启用" : "已停用");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "操作失败：" + e.getMessage());
        }
        return response;
    }
    
    // 删除会议室
    @DeleteMapping("/admin/delete/{roomId}")
    public Map<String, Object> deleteRoom(@PathVariable Long roomId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MeetingRoom room = meetingRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return response;
            }
            
            meetingRoomRepository.delete(room);
            response.put("success", true);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败：" + e.getMessage());
        }
        return response;
    }
    
    // 编辑会议室
    @PostMapping("/admin/update")
    public Map<String, Object> updateRoom(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long roomId = Long.parseLong(body.get("roomId"));
            String name = body.get("name");
            Integer capacity = Integer.parseInt(body.get("capacity"));
            String location = body.get("location");
            String facilities = body.get("facilities");
            
            MeetingRoom room = meetingRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return response;
            }
            
            room.setName(name);
            room.setCapacity(capacity);
            room.setLocation(location);
            room.setFacilities(facilities);
            
            meetingRoomRepository.save(room);
            
            response.put("success", true);
            response.put("message", "更新成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败：" + e.getMessage());
        }
        return response;
    }
}