package com.meeting.secretary.controller;

import com.meeting.secretary.dto.ApiResponse;
import com.meeting.secretary.dto.DepartmentDTO;
import com.meeting.secretary.entity.Department;
import com.meeting.secretary.service.DepartmentService;
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
 * REST Controller for Department operations
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "API quản lý phòng ban")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Lấy danh sách tất cả phòng ban")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng ban thành công", departments));
    }

    @Operation(summary = "Lấy thông tin phòng ban theo ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng ban")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentById(
            @Parameter(description = "ID của phòng ban") @PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(department));
    }

    @Operation(summary = "Tạo phòng ban mới")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phòng ban đã tồn tại")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Department>> createDepartment(
            @Valid @RequestBody DepartmentDTO dto) {
        Department department = departmentService.createDepartment(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phòng ban thành công", department));
    }

    @Operation(summary = "Cập nhật thông tin phòng ban")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng ban"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên phòng ban đã tồn tại")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @Parameter(description = "ID của phòng ban") @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO dto) {
        Department department = departmentService.updateDepartment(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng ban thành công", department));
    }

    @Operation(summary = "Xóa phòng ban")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng ban")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @Parameter(description = "ID của phòng ban") @PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng ban thành công", null));
    }
}
