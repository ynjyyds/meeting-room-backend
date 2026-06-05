package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private MailService mailService;

    // 创建预约（改为待审核状态）
    @PostMapping("/create")
    public Map<String, Object> createBooking(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = Long.parseLong(body.get("userId"));
            Long roomId = Long.parseLong(body.get("roomId"));
            String subject = body.get("subject");
            String startTimeStr = body.get("startTime");
            String endTimeStr = body.get("endTime");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);
            
            if (startTime.isAfter(endTime) || startTime.isBefore(LocalDateTime.now())) {
                response.put("success", false);
                response.put("message", "预约时间不合法");
                return response;
            }
            
            if (!meetingRoomRepository.existsById(roomId)) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return response;
            }
            
            // 只检查已通过的预约是否冲突
            List<Booking> conflicts = bookingRepository.findConflictingApprovedBookings(roomId, startTime, endTime);
            if (!conflicts.isEmpty()) {
                response.put("success", false);
                response.put("message", "该会议室在此时间段已被预约（已通过的预约）");
                return response;
            }
            
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setRoomId(roomId);
            booking.setSubject(subject);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            booking.setStatus("PENDING");  // 待审核
            booking.setCreateTime(LocalDateTime.now());
            
            bookingRepository.save(booking);
            
            // 获取会议室名称
            String roomName = meetingRoomRepository.findById(roomId).get().getName();
            String timeStr = startTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            
            response.put("success", true);
            response.put("message", "预约申请已提交，请等待管理员审核");
            response.put("bookingId", booking.getId());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "预约失败：" + e.getMessage());
        }
        
        return response;
    }
    
    // 取消预约（普通用户）- 只能取消已通过的预约
    @PostMapping("/cancel")
    public Map<String, Object> cancelBooking(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long bookingId = Long.parseLong(body.get("bookingId"));
            Long userId = Long.parseLong(body.get("userId"));
            
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.put("success", false);
                response.put("message", "预约不存在");
                return response;
            }
            
            if (!booking.getUserId().equals(userId)) {
                response.put("success", false);
                response.put("message", "只能取消自己的预约");
                return response;
            }
            
            if (!"APPROVED".equals(booking.getStatus())) {
                response.put("success", false);
                response.put("message", "只有已通过的预约才能取消");
                return response;
            }
            
            String roomName = meetingRoomRepository.findById(booking.getRoomId()).get().getName();
            User user = userRepository.findById(userId).orElse(null);
            
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            
            // 发送邮件通知
            if (mailService != null && user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                mailService.sendBookingNotification(user.getEmail(), user.getUsername(), "预约取消通知", 
                    "您已取消「" + roomName + "」的预约");
            }
            
            response.put("success", true);
            response.put("message", "取消成功");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "取消失败：" + e.getMessage());
        }
        
        return response;
    }
    
    // 查询我的预约
    @GetMapping("/my/{userId}")
    public List<Map<String, Object>> getMyBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream().map(booking -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", booking.getId());
            item.put("subject", booking.getSubject());
            item.put("startTime", booking.getStartTime());
            item.put("endTime", booking.getEndTime());
            item.put("status", getStatusText(booking.getStatus()));
            item.put("roomId", booking.getRoomId());
            item.put("userId", booking.getUserId());
            item.put("rejectReason", booking.getRejectReason());
            
            meetingRoomRepository.findById(booking.getRoomId()).ifPresent(room -> {
                item.put("roomName", room.getName());
            });
            
            return item;
        }).collect(Collectors.toList());
    }
    
    // 查询某个会议室的预约（管理员用）
    @GetMapping("/room/{roomId}")
    public List<Booking> getRoomBookings(@PathVariable Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    // ==================== 管理员专属功能 ====================
    
    // 管理员审核预约（通过/拒绝）
    @PostMapping("/admin/review")
    public Map<String, Object> reviewBooking(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long bookingId = Long.parseLong(body.get("bookingId"));
            String action = body.get("action");
            String rejectReason = body.get("rejectReason");
            
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.put("success", false);
                response.put("message", "预约不存在");
                return response;
            }
            
            if (!"PENDING".equals(booking.getStatus())) {
                response.put("success", false);
                response.put("message", "该预约已被处理，请刷新页面");
                return response;
            }
            
            User user = userRepository.findById(booking.getUserId()).orElse(null);
            String roomName = meetingRoomRepository.findById(booking.getRoomId()).get().getName();
            
            if ("approve".equals(action)) {
                // 检查时间冲突（通过前再次检查）
                List<Booking> conflicts = bookingRepository.findConflictingApprovedBookings(
                    booking.getRoomId(), booking.getStartTime(), booking.getEndTime());
                if (!conflicts.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "该时间段已被其他已通过的预约占用，无法通过");
                    return response;
                }
                
                booking.setStatus("APPROVED");
                
                // 发送邮件通知用户
                if (mailService != null && user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String emailContent = "您的预约已通过审核！\n" +
                            "会议室：「" + roomName + "」\n" +
                            "会议主题：" + booking.getSubject() + "\n" +
                            "开始时间：" + booking.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                            "结束时间：" + booking.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    mailService.sendBookingNotification(user.getEmail(), user.getUsername(), "预约审核通过", emailContent);
                }
                
                response.put("message", "已通过");
            } else {
                booking.setStatus("REJECTED");
                booking.setRejectReason(rejectReason);
                
                // 发送拒绝通知邮件
                if (mailService != null && user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String emailContent = "您的预约已被拒绝。\n" +
                            "会议室：「" + roomName + "」\n" +
                            "会议主题：" + booking.getSubject() + "\n" +
                            "开始时间：" + booking.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                            "拒绝原因：" + rejectReason;
                    mailService.sendBookingNotification(user.getEmail(), user.getUsername(), "预约被拒绝", emailContent);
                }
                
                response.put("message", "已拒绝");
            }
            
            bookingRepository.save(booking);
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "操作失败：" + e.getMessage());
        }
        return response;
    }
    
    // 获取待审核的预约（管理员）
    @GetMapping("/admin/pending")
    public List<Map<String, Object>> getPendingBookings() {
        List<Booking> bookings = bookingRepository.findByStatus("PENDING");
        return bookings.stream().map(booking -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", booking.getId());
            item.put("subject", booking.getSubject());
            item.put("startTime", booking.getStartTime());
            item.put("endTime", booking.getEndTime());
            
            String roomName = meetingRoomRepository.findById(booking.getRoomId())
                    .map(MeetingRoom::getName).orElse("未知");
            item.put("roomName", roomName);
            
            String username = userRepository.findById(booking.getUserId())
                    .map(User::getUsername).orElse("未知");
            item.put("username", username);
            
            return item;
        }).collect(Collectors.toList());
    }
    
    // 管理员取消任意预约
    @PostMapping("/admin/cancel")
    public Map<String, Object> adminCancelBooking(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long bookingId = Long.parseLong(body.get("bookingId"));
            
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.put("success", false);
                response.put("message", "预约不存在");
                return response;
            }
            
            Long userId = booking.getUserId();
            String roomName = meetingRoomRepository.findById(booking.getRoomId()).get().getName();
            User user = userRepository.findById(userId).orElse(null);
            
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            
            // 发送邮件通知
            if (mailService != null && user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                mailService.sendBookingNotification(user.getEmail(), user.getUsername(), "预约被取消通知", 
                    "管理员已取消您的「" + roomName + "」预约");
            }
            
            response.put("success", true);
            response.put("message", "已取消该预约");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "取消失败：" + e.getMessage());
        }
        
        return response;
    }
    
    // 管理员获取所有预约（带用户姓名、会议室名称）
    @GetMapping("/admin/all")
    public List<Map<String, Object>> getAllBookingsForAdmin() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream().map(booking -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", booking.getId());
            item.put("subject", booking.getSubject());
            item.put("startTime", booking.getStartTime());
            item.put("endTime", booking.getEndTime());
            item.put("status", booking.getStatus());
            item.put("roomId", booking.getRoomId());
            item.put("userId", booking.getUserId());
            item.put("rejectReason", booking.getRejectReason());
            
            // 获取会议室名称
            meetingRoomRepository.findById(booking.getRoomId()).ifPresent(room -> {
                item.put("roomName", room.getName());
            });
            
            // 获取用户名
            userRepository.findById(booking.getUserId()).ifPresent(user -> {
                item.put("username", user.getUsername());
            });
            
            return item;
        }).collect(Collectors.toList());
    }

    // ==================== 统计功能 ====================
    
    // 获取会议室使用率统计（只统计已通过的预约）
    @GetMapping("/admin/stats/room-usage")
    public Map<String, Object> getRoomUsageStats() {
        Map<String, Object> response = new HashMap<>();
        
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        List<Booking> bookings = bookingRepository.findByStatus("APPROVED");
        
        // 统计每个会议室的预约次数
        Map<Long, Integer> roomBookingCount = new HashMap<>();
        for (Booking booking : bookings) {
            roomBookingCount.put(booking.getRoomId(), 
                roomBookingCount.getOrDefault(booking.getRoomId(), 0) + 1);
        }
        
        List<String> roomNames = new ArrayList<>();
        List<Integer> bookingCounts = new ArrayList<>();
        
        for (MeetingRoom room : rooms) {
            roomNames.add(room.getName());
            bookingCounts.add(roomBookingCount.getOrDefault(room.getId(), 0));
        }
        
        response.put("roomNames", roomNames);
        response.put("bookingCounts", bookingCounts);
        return response;
    }
    
    // 获取时段热门度统计（只统计已通过的预约）
    @GetMapping("/admin/stats/hourly")
    public Map<String, Object> getHourlyStats() {
        Map<String, Object> response = new HashMap<>();
        
        List<Booking> bookings = bookingRepository.findByStatus("APPROVED");
        int[] hourlyCount = new int[24];
        
        for (Booking booking : bookings) {
            int hour = booking.getStartTime().getHour();
            hourlyCount[hour]++;
        }
        
        List<String> hours = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(i + ":00");
            counts.add(hourlyCount[i]);
        }
        
        response.put("hours", hours);
        response.put("counts", counts);
        return response;
    }

    // ==================== Excel 导出功能 ====================
    
    // 导出所有预约记录为 Excel（管理员）
    @GetMapping("/admin/export")
    public void exportBookings(HttpServletResponse response) throws Exception {
        List<Booking> bookings = bookingRepository.findAll();
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bookings.xlsx");
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("预约记录");
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "会议主题", "会议室", "用户名", "开始时间", "结束时间", "状态"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int rowNum = 1;
        for (Booking booking : bookings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(booking.getId());
            row.createCell(1).setCellValue(booking.getSubject());
            
            String roomName = meetingRoomRepository.findById(booking.getRoomId())
                    .map(MeetingRoom::getName).orElse("未知");
            row.createCell(2).setCellValue(roomName);
            
            String username = userRepository.findById(booking.getUserId())
                    .map(User::getUsername).orElse("未知");
            row.createCell(3).setCellValue(username);
            
            row.createCell(4).setCellValue(booking.getStartTime().toString());
            row.createCell(5).setCellValue(booking.getEndTime().toString());
            row.createCell(6).setCellValue(getStatusText(booking.getStatus()));
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    // 辅助方法：状态转文字
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "待审核";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已拒绝";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
}