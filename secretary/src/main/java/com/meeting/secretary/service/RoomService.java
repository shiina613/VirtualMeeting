package com.meeting.secretary.service;

import com.meeting.secretary.dto.RoomDTO;
import com.meeting.secretary.entity.Room;
import com.meeting.secretary.exception.DuplicateResourceException;
import com.meeting.secretary.exception.ResourceNotFoundException;
import com.meeting.secretary.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Room operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;

    /**
     * Get all rooms
     */
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get room by ID
     */
    @Transactional(readOnly = true)
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng họp", id));
    }

    /**
     * Create new room
     */
    public Room createRoom(RoomDTO dto) {
        if (roomRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Phòng họp với tên '" + dto.getName() + "' đã tồn tại");
        }

        Room room = new Room();
        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());

        return roomRepository.save(room);
    }

    /**
     * Update existing room
     */
    public Room updateRoom(Long id, RoomDTO dto) {
        Room room = getRoomById(id);

        // Check if name already exists for another room
        roomRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Phòng họp với tên '" + dto.getName() + "' đã tồn tại");
                    }
                });

        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());

        return roomRepository.save(room);
    }

    /**
     * Delete room by ID
     */
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Phòng họp", id);
        }
        roomRepository.deleteById(id);
    }
}
