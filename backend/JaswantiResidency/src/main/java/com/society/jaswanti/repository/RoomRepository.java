package com.society.jaswanti.repository;

import com.society.jaswanti.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r FROM Room r WHERE EXISTS (SELECT 1 FROM RoomResident rr WHERE rr.room = r AND rr.resident.id = :residentId AND rr.isActive = true) OR (NOT EXISTS (SELECT 1 FROM RoomResident rr2 WHERE rr2.room = r) AND r.resident.id = :residentId)")
    List<Room> findByResidentId(@org.springframework.data.repository.query.Param("residentId") Long residentId);

    Optional<Room> findByRoomNumber(String roomNumber);
}
