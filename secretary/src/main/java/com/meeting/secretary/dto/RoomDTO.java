package com.meeting.secretary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Room create/update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {

    private Long id;

    @NotBlank(message = "Tên phòng họp không được để trống")
    private String name;

    private String description;

    @Positive(message = "Sức chứa phải là số dương")
    private Integer capacity;

    private String location;
}
