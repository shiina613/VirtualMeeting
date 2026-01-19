package com.meeting.secretary.controller;

import com.meeting.secretary.dto.ApiResponse;
import com.meeting.secretary.dto.RoomDTO;
import com.meeting.secretary.entity.Room;
import com.meeting.secretary.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Room operations
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room", description = "API quản lý phòng họp")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "Lấy danh sách tất cả phòng họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<Room>>> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng họp thành công", rooms));
    }

    @Operation(summary = "Lấy thông tin phòng họp theo ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng họp")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> getRoomById(
            @Parameter(description = "ID của phòng họp") @PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @Operation(summary = "Tạo phòng họp mới")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phòng họp đã tồn tại")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Room>> createRoom(
            @Valid @RequestBody RoomDTO dto) {
        Room room = roomService.createRoom(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phòng họp thành công", room));
    }

    @Operation(summary = "Cập nhật thông tin phòng họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng họp"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên phòng họp đã tồn tại")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> updateRoom(
            @Parameter(description = "ID của phòng họp") @PathVariable Long id,
            @Valid @RequestBody RoomDTO dto) {
        Room room = roomService.updateRoom(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng họp thành công", room));
    }

    @Operation(summary = "Xóa phòng họp")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng họp")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @Parameter(description = "ID của phòng họp") @PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng họp thành công", null));
    }
}
