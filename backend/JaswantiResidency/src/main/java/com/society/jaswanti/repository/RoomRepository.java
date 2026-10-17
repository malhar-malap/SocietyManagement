package com.society.jaswanti.repository;

import com.society.jaswanti.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByResidentId(Long residentId);
    Optional<Room> findByRoomNumber(String roomNumber);
}
