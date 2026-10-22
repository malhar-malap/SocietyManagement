package com.society.jaswanti.repository;

import com.society.jaswanti.entity.RoomResident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomResidentRepository extends JpaRepository<RoomResident, Long> {

    List<RoomResident> findByRoomId(Long roomId);

    List<RoomResident> findByRoomIdAndIsActiveTrue(Long roomId);

    List<RoomResident> findByResidentId(Long residentId);

    List<RoomResident> findByResidentIdAndIsActiveTrue(Long residentId);

    Optional<RoomResident> findByRoomIdAndResidentIdAndIsActiveTrue(Long roomId, Long residentId);

    Optional<RoomResident> findByRoomIdAndIsActiveTrueAndIsPrimaryContactTrue(Long roomId);

    Optional<RoomResident> findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(Long roomId);

    boolean existsByRoomIdAndResidentIdAndIsActiveTrue(Long roomId, Long residentId);

    boolean existsByRoomId(Long roomId);

    boolean existsByResidentId(Long residentId);
}
