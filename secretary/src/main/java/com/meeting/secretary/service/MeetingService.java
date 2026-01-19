package com.meeting.secretary.service;

import com.meeting.secretary.dto.MeetingDTO;
import com.meeting.secretary.dto.MeetingStatisticsDTO;
import com.meeting.secretary.entity.Meeting;
import com.meeting.secretary.entity.MeetingStatus;
import com.meeting.secretary.exception.ResourceNotFoundException;
import com.meeting.secretary.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Meeting operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;

    /**
     * Get all meetings
     */
    @Transactional(readOnly = true)
    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    /**
     * Get meeting by ID
     */
    @Transactional(readOnly = true)
    public Meeting getMeetingById(Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuộc họp", id));
    }

    /**
     * Get meetings by status
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByStatus(MeetingStatus status) {
        return meetingRepository.findByStatus(status);
    }

    /**
     * Get meetings by department
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByDepartment(String department) {
        return meetingRepository.findByDepartment(department);
    }

    /**
     * Get meetings by room
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByRoom(String room) {
        return meetingRepository.findByRoom(room);
    }

    /**
     * Get meetings by date
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return meetingRepository.findByDateRange(start, end);
    }

    /**
     * Get meetings by month
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByMonth(int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.with(TemporalAdjusters.firstDayOfNextMonth());
        return meetingRepository.findByDateRange(start, end);
    }

    /**
     * Get meetings by year
     */
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByYear(int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year + 1, 1, 1, 0, 0);
        return meetingRepository.findByDateRange(start, end);
    }

    /**
     * Create new meeting
     */
    public Meeting createMeeting(MeetingDTO dto) {
        validateMeetingTime(dto.getStartTime(), dto.getEndTime());

        Meeting meeting = new Meeting();
        mapDtoToEntity(dto, meeting);
        
        // Default status is SCHEDULED if not provided
        if (meeting.getStatus() == null) {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }

        return meetingRepository.save(meeting);
    }

    /**
     * Update existing meeting
     */
    public Meeting updateMeeting(Long id, MeetingDTO dto) {
        Meeting meeting = getMeetingById(id);
        validateMeetingTime(dto.getStartTime(), dto.getEndTime());
        mapDtoToEntity(dto, meeting);

        return meetingRepository.save(meeting);
    }

    /**
     * Update meeting status
     */
    public Meeting updateMeetingStatus(Long id, MeetingStatus status) {
        Meeting meeting = getMeetingById(id);
        meeting.setStatus(status);
        return meetingRepository.save(meeting);
    }

    /**
     * Delete meeting by ID
     */
    public void deleteMeeting(Long id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cuộc họp", id);
        }
        meetingRepository.deleteById(id);
    }

    /**
     * Get comprehensive meeting statistics
     */
    @Transactional(readOnly = true)
    public MeetingStatisticsDTO getStatistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        
        LocalDateTime weekStart = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusDays(7);
        
        LocalDateTime monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime monthEnd = today.with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay();
        
        LocalDateTime yearStart = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
        LocalDateTime yearEnd = today.with(TemporalAdjusters.firstDayOfNextYear()).atStartOfDay();

        // Get grouped statistics
        Map<String, Long> byDepartment = new HashMap<>();
        meetingRepository.countByDepartmentGrouped().forEach(row -> 
            byDepartment.put((String) row[0], (Long) row[1]));

        Map<String, Long> byRoom = new HashMap<>();
        meetingRepository.countByRoomGrouped().forEach(row -> 
            byRoom.put((String) row[0], (Long) row[1]));

        Map<String, Long> byStatus = new HashMap<>();
        meetingRepository.countByStatusGrouped().forEach(row -> 
            byStatus.put(((MeetingStatus) row[0]).name(), (Long) row[1]));

        return MeetingStatisticsDTO.builder()
                .totalMeetings(meetingRepository.count())
                .scheduledMeetings(meetingRepository.countByStatus(MeetingStatus.SCHEDULED))
                .ongoingMeetings(meetingRepository.countByStatus(MeetingStatus.ONGOING))
                .finishedMeetings(meetingRepository.countByStatus(MeetingStatus.FINISHED))
                .meetingsToday(meetingRepository.countByDateRange(todayStart, todayEnd))
                .meetingsThisWeek(meetingRepository.countByDateRange(weekStart, weekEnd))
                .meetingsThisMonth(meetingRepository.countByDateRange(monthStart, monthEnd))
                .meetingsThisYear(meetingRepository.countByDateRange(yearStart, yearEnd))
                .byDepartment(byDepartment)
                .byRoom(byRoom)
                .byStatus(byStatus)
                .build();
    }

    /**
     * Get statistics for a specific date
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", meetingRepository.countByDateRange(start, end));
        stats.put("scheduled", meetingRepository.countByStatusAndDateRange(MeetingStatus.SCHEDULED, start, end));
        stats.put("ongoing", meetingRepository.countByStatusAndDateRange(MeetingStatus.ONGOING, start, end));
        stats.put("finished", meetingRepository.countByStatusAndDateRange(MeetingStatus.FINISHED, start, end));
        
        return stats;
    }

    /**
     * Get statistics for a specific month
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByMonth(int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.with(TemporalAdjusters.firstDayOfNextMonth());
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", meetingRepository.countByDateRange(start, end));
        stats.put("scheduled", meetingRepository.countByStatusAndDateRange(MeetingStatus.SCHEDULED, start, end));
        stats.put("ongoing", meetingRepository.countByStatusAndDateRange(MeetingStatus.ONGOING, start, end));
        stats.put("finished", meetingRepository.countByStatusAndDateRange(MeetingStatus.FINISHED, start, end));
        
        return stats;
    }

    /**
     * Get statistics for a specific year
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByYear(int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year + 1, 1, 1, 0, 0);
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", meetingRepository.countByDateRange(start, end));
        stats.put("scheduled", meetingRepository.countByStatusAndDateRange(MeetingStatus.SCHEDULED, start, end));
        stats.put("ongoing", meetingRepository.countByStatusAndDateRange(MeetingStatus.ONGOING, start, end));
        stats.put("finished", meetingRepository.countByStatusAndDateRange(MeetingStatus.FINISHED, start, end));
        
        return stats;
    }

    // Helper methods
    private void mapDtoToEntity(MeetingDTO dto, Meeting meeting) {
        meeting.setTitle(dto.getTitle());
        meeting.setDescription(dto.getDescription());
        meeting.setStartTime(dto.getStartTime());
        meeting.setEndTime(dto.getEndTime());
        meeting.setDepartment(dto.getDepartment());
        meeting.setRoom(dto.getRoom());
        meeting.setChairman(dto.getChairman());
        meeting.setSecretary(dto.getSecretary());
        if (dto.getStatus() != null) {
            meeting.setStatus(dto.getStatus());
        }
    }

    private void validateMeetingTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }
}
