package com.meeting.secretary.entity;

/**
 * Enum representing the status of a meeting
 */
public enum MeetingStatus {
    SCHEDULED("Đã lên lịch"),
    ONGOING("Đang diễn ra"),
    FINISHED("Đã kết thúc");

    private final String displayName;

    MeetingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
