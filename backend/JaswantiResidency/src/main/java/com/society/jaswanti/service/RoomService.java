package com.society.jaswanti.service;

import com.society.jaswanti.dto.AssignResidentRequest;
import com.society.jaswanti.dto.RoomResidentDto;
import com.society.jaswanti.entity.RelationshipType;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.RoomResident;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.dto.CorrectOccupancyDatesRequest;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public RoomService(RoomRepository roomRepository,
                       RoomResidentRepository roomResidentRepository,
                       UserRepository userRepository,
                       ActivityLogService activityLogService) {
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public List<Room> getRoomsByResidentId(Long residentId) {
        return roomRepository.findByResidentId(residentId);
    }

    public List<RoomResidentDto> getRoomOccupants(Long roomId) {
        return roomResidentRepository.findByRoomId(roomId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<RoomResidentDto> getActiveRoomOccupants(Long roomId) {
        return roomResidentRepository.findByRoomIdAndIsActiveTrue(roomId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<RoomResidentDto> getActiveRoomOccupantsForResident(Long roomId) {
        return roomResidentRepository.findByRoomIdAndIsActiveTrue(roomId).stream()
                .map(this::convertToResidentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomResidentDto assignResidentToRoom(Long roomId, AssignResidentRequest request) {
        if (request == null || request.getResidentId() == null) {
            throw new IllegalArgumentException("Resident ID is required");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));
        User resident = userRepository.findById(request.getResidentId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getResidentId()));

        if (resident.getRole() != Role.RESIDENT) {
            throw new IllegalArgumentException("Only users with role RESIDENT can be assigned as room occupants");
        }
        if (!Boolean.TRUE.equals(resident.isActive()) || (resident.getStatus() != null && resident.getStatus() != UserStatus.ACTIVE)) {
            throw new IllegalArgumentException("Cannot assign an inactive or deactivated user as a room occupant");
        }

        if (roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(roomId, resident.getId())) {
            throw new IllegalArgumentException("Resident is already an active occupant of this room");
        }

        if (request.getEndDate() != null) {
            throw new IllegalArgumentException("An active relationship must not have an end date");
        }

        RelationshipType type = RelationshipType.OWNER;
        if (request.getRelationshipType() != null && !request.getRelationshipType().isBlank()) {
            try {
                type = RelationshipType.valueOf(request.getRelationshipType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid relationship type: " + request.getRelationshipType());
            }
        }

        boolean isPrimary = Boolean.TRUE.equals(request.getPrimaryContact());
        if (isPrimary) {
            // Unset other active primary contacts for this room
            roomResidentRepository.findByRoomIdAndIsActiveTrue(roomId).forEach(existing -> {
                if (existing.isPrimaryContact()) {
                    existing.setPrimaryContact(false);
                    roomResidentRepository.save(existing);
                }
            });
        } else {
            // If there are no other active occupants, make this one primary by default
            if (roomResidentRepository.findByRoomIdAndIsActiveTrue(roomId).isEmpty()) {
                isPrimary = true;
            }
        }

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        if (start.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the future");
        }

        RoomResident rr = new RoomResident();
        rr.setRoom(room);
        rr.setResident(resident);
        rr.setRelationshipType(type);
        rr.setStartDate(start);
        rr.setEndDate(null);
        rr.setActive(true);
        rr.setPrimaryContact(isPrimary);
        rr = roomResidentRepository.save(rr);

        // Keep room.resident in sync with primary contact for legacy queries
        if (isPrimary) {
            room.setResident(resident);
            roomRepository.save(room);
        }

        return convertToDto(rr);
    }

    @Transactional
    public RoomResidentDto endOccupancy(Long occupantId, LocalDate endDate) {
        RoomResident rr = roomResidentRepository.findById(occupantId)
                .orElseThrow(() -> new IllegalArgumentException("Occupant relationship not found with ID: " + occupantId));

        if (!rr.isActive()) {
            throw new IllegalArgumentException("Occupancy relationship is already inactive");
        }

        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        if (rr.getStartDate() != null && effectiveEndDate.isBefore(rr.getStartDate())) {
            throw new IllegalArgumentException("End date cannot precede start date");
        }

        rr.setActive(false);
        rr.setEndDate(effectiveEndDate);
        if (rr.isPrimaryContact()) {
            rr.setPrimaryContact(false);
        }
        rr = roomResidentRepository.save(rr);

        // If this was room's primary contact, point room.resident to another active occupant or null
        Room room = rr.getRoom();
        if (room != null) {
            List<RoomResident> activeOccupants = roomResidentRepository.findByRoomIdAndIsActiveTrue(room.getId());
            if (!activeOccupants.isEmpty()) {
                RoomResident newPrimary = activeOccupants.stream()
                        .filter(RoomResident::isPrimaryContact)
                        .findFirst()
                        .orElse(activeOccupants.get(0));
                newPrimary.setPrimaryContact(true);
                roomResidentRepository.save(newPrimary);
                room.setResident(newPrimary.getResident());
            } else {
                room.setResident(null);
            }
            roomRepository.save(room);
        }

        return convertToDto(rr);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoomResidentDto correctOccupancyDates(Long occupantId, CorrectOccupancyDatesRequest request, Authentication auth) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Correction reason is required and cannot be blank");
        }

        if (auth == null || auth.getName() == null || auth.getName().trim().isEmpty()) {
            throw new AccessDeniedException("Authenticated actor is required for occupancy date correction");
        }
        User actor = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated actor not found in user directory: " + auth.getName()));

        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MASTER_ADMIN) {
            throw new AccessDeniedException("Only Admin or Master Admin can perform occupancy date corrections");
        }

        RoomResident rr = roomResidentRepository.findById(occupantId)
                .orElseThrow(() -> new IllegalArgumentException("Occupant relationship not found with ID: " + occupantId));

        if (request.getStartDate() != null && request.getStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the future");
        }

        if (rr.isActive()) {
            if (request.getEndDate() != null) {
                throw new IllegalArgumentException("Cannot set an end date on an active occupancy record. Use end-occupancy workflow instead.");
            }
        } else {
            LocalDate effectiveStartDate = request.getStartDate();
            LocalDate effectiveEndDate = request.getEndDate() != null ? request.getEndDate() : rr.getEndDate();
            if (effectiveStartDate != null && effectiveEndDate != null && effectiveEndDate.isBefore(effectiveStartDate)) {
                throw new IllegalArgumentException("End date cannot precede start date");
            }
            if (request.getEndDate() != null) {
                rr.setEndDate(request.getEndDate());
            }
        }

        LocalDate oldStart = rr.getStartDate();
        LocalDate oldEnd = rr.getEndDate();

        rr.setStartDate(request.getStartDate());
        rr = roomResidentRepository.save(rr);

        String unitNumber = rr.getRoom() != null ? rr.getRoom().getRoomNumber() : "unknown";
        String residentName = rr.getResident() != null ? rr.getResident().getName() : "unknown";
        String desc = String.format("Corrected occupancy dates for unit %s, resident %s (ID: %d). " +
                        "StartDate: [%s -> %s], EndDate: [%s -> %s]. Reason: %s",
                unitNumber,
                residentName,
                rr.getId(),
                oldStart != null ? oldStart.toString() : "null",
                rr.getStartDate() != null ? rr.getStartDate().toString() : "null",
                oldEnd != null ? oldEnd.toString() : "null",
                rr.getEndDate() != null ? rr.getEndDate().toString() : "null",
                request.getReason().trim());

        activityLogService.logActivity(actor, "OCCUPANCY_DATE_CORRECTION", "REGISTRY", desc);

        return convertToDto(rr);
    }

    public String resolveResidentName(Room room) {
        if (room == null) return null;
        Optional<RoomResident> active = roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(room.getId());
        if (active.isPresent()) {
            return active.get().getResident() != null ? active.get().getResident().getName() : null;
        }
        if (!roomResidentRepository.existsByRoomId(room.getId())) {
            return room.getResident() != null ? room.getResident().getName() : null;
        }
        return null;
    }

    public Long resolveResidentId(Room room) {
        if (room == null) return null;
        Optional<RoomResident> active = roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(room.getId());
        if (active.isPresent()) {
            return active.get().getResident() != null ? active.get().getResident().getId() : null;
        }
        if (!roomResidentRepository.existsByRoomId(room.getId())) {
            return room.getResident() != null ? room.getResident().getId() : null;
        }
        return null;
    }

    public RoomResidentDto convertToDto(RoomResident rr) {
        RoomResidentDto dto = new RoomResidentDto();
        dto.setId(rr.getId());
        if (rr.getRoom() != null) {
            dto.setRoomId(rr.getRoom().getId());
            dto.setRoomNumber(rr.getRoom().getRoomNumber());
        }
        if (rr.getResident() != null) {
            dto.setResidentId(rr.getResident().getId());
            dto.setResidentName(rr.getResident().getName());
            dto.setResidentEmail(rr.getResident().getEmail());
            dto.setResidentPhone(rr.getResident().getPhone());
        }
        dto.setRelationshipType(rr.getRelationshipType() != null ? rr.getRelationshipType().name() : "OWNER");
        dto.setStartDate(rr.getStartDate());
        dto.setEndDate(rr.getEndDate());
        dto.setActive(rr.isActive());
        dto.setPrimaryContact(rr.isPrimaryContact());
        return dto;
    }

    public RoomResidentDto convertToResidentDto(RoomResident rr) {
        RoomResidentDto dto = new RoomResidentDto();
        dto.setId(rr.getId());
        if (rr.getRoom() != null) {
            dto.setRoomId(rr.getRoom().getId());
            dto.setRoomNumber(rr.getRoom().getRoomNumber());
        }
        if (rr.getResident() != null) {
            dto.setResidentId(rr.getResident().getId());
            dto.setResidentName(rr.getResident().getName());
            dto.setResidentEmail(rr.getResident().getEmail());
            dto.setResidentPhone(rr.getResident().getPhone());
        }
        dto.setRelationshipType(rr.getRelationshipType() != null ? rr.getRelationshipType().name() : "OWNER");
        dto.setStartDate(rr.getStartDate());
        dto.setEndDate(null);
        dto.setActive(true);
        dto.setPrimaryContact(rr.isPrimaryContact());
        return dto;
    }
}
