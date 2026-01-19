package com.meeting.secretary.controller;

import com.meeting.secretary.dto.ApiResponse;
import com.meeting.secretary.dto.MeetingDTO;
import com.meeting.secretary.dto.MeetingStatisticsDTO;
import com.meeting.secretary.entity.Meeting;
import com.meeting.secretary.entity.MeetingStatus;
import com.meeting.secretary.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Meeting operations
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting", description = "API quản lý cuộc họp")
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingService meetingService;

    // ==================== CRUD Operations ====================

    @Operation(summary = "Lấy danh sách tất cả cuộc họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<Meeting>>> getAllMeetings() {
        List<Meeting> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cuộc họp thành công", meetings));
    }

    @Operation(summary = "Lấy thông tin cuộc họp theo ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc họp")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Meeting>> getMeetingById(
            @Parameter(description = "ID của cuộc họp") @PathVariable Long id) {
        Meeting meeting = meetingService.getMeetingById(id);
        return ResponseEntity.ok(ApiResponse.success(meeting));
    }

    @Operation(summary = "Tạo cuộc họp mới")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Meeting>> createMeeting(
            @Valid @RequestBody MeetingDTO dto) {
        Meeting meeting = meetingService.createMeeting(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cuộc họp thành công", meeting));
    }

    @Operation(summary = "Cập nhật thông tin cuộc họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc họp")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Meeting>> updateMeeting(
            @Parameter(description = "ID của cuộc họp") @PathVariable Long id,
            @Valid @RequestBody MeetingDTO dto) {
        Meeting meeting = meetingService.updateMeeting(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cuộc họp thành công", meeting));
    }

    @Operation(summary = "Cập nhật trạng thái cuộc họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc họp")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Meeting>> updateMeetingStatus(
            @Parameter(description = "ID của cuộc họp") @PathVariable Long id,
            @Parameter(description = "Trạng thái mới") @RequestParam MeetingStatus status) {
        Meeting meeting = meetingService.updateMeetingStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái cuộc họp thành công", meeting));
    }

    @Operation(summary = "Xóa cuộc họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc họp")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @Parameter(description = "ID của cuộc họp") @PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cuộc họp thành công", null));
    }

    // ==================== Filter Operations ====================

    @Operation(summary = "Lấy danh sách cuộc họp theo trạng thái")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByStatus(
            @Parameter(description = "Trạng thái cuộc họp (SCHEDULED, ONGOING, FINISHED)") 
            @PathVariable MeetingStatus status) {
        List<Meeting> meetings = meetingService.getMeetingsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    @Operation(summary = "Lấy danh sách cuộc họp theo phòng ban")
    @GetMapping("/department/{department}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByDepartment(
            @Parameter(description = "Tên phòng ban") @PathVariable String department) {
        List<Meeting> meetings = meetingService.getMeetingsByDepartment(department);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    @Operation(summary = "Lấy danh sách cuộc họp theo phòng họp")
    @GetMapping("/room/{room}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByRoom(
            @Parameter(description = "Tên phòng họp") @PathVariable String room) {
        List<Meeting> meetings = meetingService.getMeetingsByRoom(room);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    @Operation(summary = "Lấy danh sách cuộc họp theo ngày")
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByDate(
            @Parameter(description = "Ngày (yyyy-MM-dd)") 
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Meeting> meetings = meetingService.getMeetingsByDate(date);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    @Operation(summary = "Lấy danh sách cuộc họp theo tháng")
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByMonth(
            @Parameter(description = "Năm") @PathVariable int year,
            @Parameter(description = "Tháng (1-12)") @PathVariable int month) {
        List<Meeting> meetings = meetingService.getMeetingsByMonth(year, month);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    @Operation(summary = "Lấy danh sách cuộc họp theo năm")
    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMeetingsByYear(
            @Parameter(description = "Năm") @PathVariable int year) {
        List<Meeting> meetings = meetingService.getMeetingsByYear(year);
        return ResponseEntity.ok(ApiResponse.success(meetings));
    }

    // ==================== Statistics Operations ====================

    @Operation(summary = "Lấy thống kê tổng hợp cuộc họp")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<MeetingStatisticsDTO>> getStatistics() {
        MeetingStatisticsDTO statistics = meetingService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thành công", statistics));
    }

    @Operation(summary = "Lấy thống kê cuộc họp theo ngày")
    @GetMapping("/statistics/date/{date}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatisticsByDate(
            @Parameter(description = "Ngày (yyyy-MM-dd)") 
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Long> statistics = meetingService.getStatisticsByDate(date);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @Operation(summary = "Lấy thống kê cuộc họp theo tháng")
    @GetMapping("/statistics/month/{year}/{month}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatisticsByMonth(
            @Parameter(description = "Năm") @PathVariable int year,
            @Parameter(description = "Tháng (1-12)") @PathVariable int month) {
        Map<String, Long> statistics = meetingService.getStatisticsByMonth(year, month);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @Operation(summary = "Lấy thống kê cuộc họp theo năm")
    @GetMapping("/statistics/year/{year}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatisticsByYear(
            @Parameter(description = "Năm") @PathVariable int year) {
        Map<String, Long> statistics = meetingService.getStatisticsByYear(year);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
