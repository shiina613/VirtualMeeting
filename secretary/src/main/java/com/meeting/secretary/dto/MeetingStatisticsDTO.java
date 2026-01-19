package com.meeting.secretary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Meeting statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingStatisticsDTO {

    private long totalMeetings;
    private long scheduledMeetings;
    private long ongoingMeetings;
    private long finishedMeetings;

    // Statistics by time period
    private long meetingsToday;
    private long meetingsThisWeek;
    private long meetingsThisMonth;
    private long meetingsThisYear;

    // Grouped statistics
    private Map<String, Long> byDepartment;
    private Map<String, Long> byRoom;
    private Map<String, Long> byStatus;
}
