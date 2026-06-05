package com.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // 查询某个时间段内某个会议室的冲突预约（已通过的预约）
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status = 'APPROVED' " +
           "AND NOT (b.endTime <= :startTime OR b.startTime >= :endTime)")
    List<Booking> findConflictingApprovedBookings(@Param("roomId") Long roomId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
    
    // 查询某个时间段内某个会议室的冲突预约（旧版，保留兼容）
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status = 'APPROVED' " +
           "AND NOT (b.endTime <= :startTime OR b.startTime >= :endTime)")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    // 查询某个用户的所有预约
    List<Booking> findByUserId(Long userId);
    
    // 查询某个会议室的所有预约
    List<Booking> findByRoomId(Long roomId);
    
    // 根据状态查询
    List<Booking> findByStatus(String status);
    
    // 查询某个用户特定状态的预约
    List<Booking> findByUserIdAndStatus(Long userId, String status);
    
    // 查询某个会议室特定状态的预约
    List<Booking> findByRoomIdAndStatus(Long roomId, String status);
    
    // 查询某个时间段内所有已通过的预约
    @Query("SELECT b FROM Booking b WHERE b.status = 'APPROVED' " +
           "AND ((b.startTime BETWEEN :start AND :end) OR (b.endTime BETWEEN :start AND :end) " +
           "OR (b.startTime <= :start AND b.endTime >= :end))")
    List<Booking> findApprovedBookingsInTimeRange(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}