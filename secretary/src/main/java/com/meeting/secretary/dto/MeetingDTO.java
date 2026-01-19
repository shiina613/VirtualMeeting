package com.meeting.secretary.dto;

import com.meeting.secretary.entity.MeetingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Meeting create/update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDTO {

    private Long id;

    @NotBlank(message = "Tiêu đề cuộc họp không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    private MeetingStatus status;

    @NotBlank(message = "Phòng ban không được để trống")
    private String department;

    @NotBlank(message = "Phòng họp không được để trống")
    private String room;

    @NotBlank(message = "Chủ tọa không được để trống")
    private String chairman;

    @NotBlank(message = "Thư ký không được để trống")
    private String secretary;
}
