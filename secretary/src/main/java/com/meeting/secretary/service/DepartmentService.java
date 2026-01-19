package com.meeting.secretary.service;

import com.meeting.secretary.dto.DepartmentDTO;
import com.meeting.secretary.entity.Department;
import com.meeting.secretary.exception.DuplicateResourceException;
import com.meeting.secretary.exception.ResourceNotFoundException;
import com.meeting.secretary.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Department operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Get all departments
     */
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * Get department by ID
     */
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", id));
    }

    /**
     * Create new department
     */
    public Department createDepartment(DepartmentDTO dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Phòng ban với tên '" + dto.getName() + "' đã tồn tại");
        }

        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());

        return departmentRepository.save(department);
    }

    /**
     * Update existing department
     */
    public Department updateDepartment(Long id, DepartmentDTO dto) {
        Department department = getDepartmentById(id);

        // Check if name already exists for another department
        departmentRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Phòng ban với tên '" + dto.getName() + "' đã tồn tại");
                    }
                });

        department.setName(dto.getName());
        department.setDescription(dto.getDescription());

        return departmentRepository.save(department);
    }

    /**
     * Delete department by ID
     */
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Phòng ban", id);
        }
        departmentRepository.deleteById(id);
    }
}
