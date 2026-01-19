package com.meeting.secretary.repository;

import com.meeting.secretary.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Room entity
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByName(String name);
    
    boolean existsByName(String name);
}
