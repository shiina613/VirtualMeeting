package com.meeting.secretary.repository;

import com.meeting.secretary.entity.Meeting;
import com.meeting.secretary.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Meeting entity
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Find by status
    List<Meeting> findByStatus(MeetingStatus status);

    // Count by status
    long countByStatus(MeetingStatus status);

    // Find meetings by date range
    @Query("SELECT m FROM Meeting m WHERE m.startTime >= :start AND m.startTime < :end")
    List<Meeting> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Count meetings by date range
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.startTime >= :start AND m.startTime < :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Find meetings by department
    List<Meeting> findByDepartment(String department);

    // Find meetings by room
    List<Meeting> findByRoom(String room);

    // Find meetings by status and date range
    @Query("SELECT m FROM Meeting m WHERE m.status = :status AND m.startTime >= :start AND m.startTime < :end")
    List<Meeting> findByStatusAndDateRange(
            @Param("status") MeetingStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Count meetings by status and date range
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.status = :status AND m.startTime >= :start AND m.startTime < :end")
    long countByStatusAndDateRange(
            @Param("status") MeetingStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Statistics by department
    @Query("SELECT m.department, COUNT(m) FROM Meeting m GROUP BY m.department")
    List<Object[]> countByDepartmentGrouped();

    // Statistics by room
    @Query("SELECT m.room, COUNT(m) FROM Meeting m GROUP BY m.room")
    List<Object[]> countByRoomGrouped();

    // Statistics by status
    @Query("SELECT m.status, COUNT(m) FROM Meeting m GROUP BY m.status")
    List<Object[]> countByStatusGrouped();
}
