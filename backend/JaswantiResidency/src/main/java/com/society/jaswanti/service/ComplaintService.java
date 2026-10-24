package com.society.jaswanti.service;

import com.society.jaswanti.dto.ComplaintDto;
import com.society.jaswanti.entity.Complaint;
import com.society.jaswanti.entity.ComplaintStatus;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final FileValidationService fileValidationService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            com.society.jaswanti.repository.RoomRepository roomRepository,
                            FileValidationService fileValidationService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.fileValidationService = fileValidationService;
    }

    public ComplaintDto createComplaint(Long residentId, String category, String description, MultipartFile file) throws IOException {
        return createSubmission(residentId, category, description, "COMPLAINT", null, null, null, file);
    }

    public ComplaintDto createSubmission(Long residentId,
                                         String category,
                                         String description,
                                         String submissionType,
                                         String permissionCategory,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         MultipartFile file) throws IOException {
        User resident = userRepository.findById(residentId).orElseThrow(() -> new RuntimeException("User not found"));

        Complaint complaint = new Complaint(resident, category != null && !category.isBlank() ? category : "GENERAL", description);
        complaint.setSubmissionType(submissionType != null && !submissionType.isBlank() ? submissionType.toUpperCase() : "COMPLAINT");
        complaint.setPermissionCategory(permissionCategory);
        complaint.setStartDate(startDate);
        complaint.setEndDate(endDate);
        complaint.setStatus(ComplaintStatus.OPEN);

        if (file != null && !file.isEmpty()) {
            String detectedType = fileValidationService.validateImageOrPdf(file);
            complaint.setImageData(file.getBytes());
            complaint.setImageFilename(file.getOriginalFilename());
            complaint.setImageContentType(detectedType);
        }

        Complaint saved = complaintRepository.save(complaint);
        if (saved.getImageData() != null && saved.getImageData().length > 0) {
            saved.setImageUrl("/api/complaints/" + saved.getId() + "/attachment");
            saved = complaintRepository.save(saved);
        }

        return convertToDto(saved);
    }

    public List<ComplaintDto> getComplaintsByResident(Long residentId) {
        return complaintRepository.findByResidentIdOrderByCreatedDateDesc(residentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ComplaintDto> updateComplaint(Long id, Complaint updatedComplaint) {
        return complaintRepository.findById(id).map(complaint -> {
            if (updatedComplaint.getCategory() != null) complaint.setCategory(updatedComplaint.getCategory());
            if (updatedComplaint.getDescription() != null) complaint.setDescription(updatedComplaint.getDescription());
            if (updatedComplaint.getSubmissionType() != null) complaint.setSubmissionType(updatedComplaint.getSubmissionType());
            if (updatedComplaint.getPermissionCategory() != null) complaint.setPermissionCategory(updatedComplaint.getPermissionCategory());
            if (updatedComplaint.getStartDate() != null) complaint.setStartDate(updatedComplaint.getStartDate());
            if (updatedComplaint.getEndDate() != null) complaint.setEndDate(updatedComplaint.getEndDate());
            return convertToDto(complaintRepository.save(complaint));
        });
    }

    public Optional<ComplaintDto> updateComplaintStatus(Long id, String status) {
        return complaintRepository.findById(id).map(complaint -> {
            complaint.setStatus(parseStatus(status));
            return convertToDto(complaintRepository.save(complaint));
        });
    }

    public Optional<ComplaintDto> respondToSubmission(Long id, String status, String adminResponse) {
        return complaintRepository.findById(id).map(complaint -> {
            if (status != null && !status.isBlank()) {
                complaint.setStatus(parseStatus(status));
            }
            if (adminResponse != null) {
                complaint.setAdminResponse(adminResponse);
            }
            return convertToDto(complaintRepository.save(complaint));
        });
    }

    public Optional<Complaint> getComplaintEntity(Long id) {
        return complaintRepository.findById(id);
    }

    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new com.society.jaswanti.exception.ResourceNotFoundException("Submission not found with ID: " + id);
        }
        complaintRepository.deleteById(id);
    }

    /** Rejects unknown statuses with a 400 instead of silently leaving the status unchanged. */
    private static ComplaintStatus parseStatus(String status) {
        try {
            return ComplaintStatus.valueOf(status == null ? "" : status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + status + "'. Allowed: "
                    + java.util.Arrays.toString(ComplaintStatus.values()));
        }
    }

    public ComplaintDto convertToDto(Complaint complaint) {
        String roomNumber = null;
        if (complaint.getResident() != null) {
            java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(complaint.getResident().getId());
            if (!rooms.isEmpty()) {
                roomNumber = rooms.get(0).getRoomNumber();
            }
        }

        String fullDesc = complaint.getDescription();
        String title = complaint.getSubmissionType() != null ? complaint.getSubmissionType() : "Complaint";
        String desc = fullDesc;

        if (fullDesc != null && fullDesc.contains(" - ")) {
            int splitIdx = fullDesc.indexOf(" - ");
            title = fullDesc.substring(0, splitIdx);
            desc = fullDesc.substring(splitIdx + 3);
        }

        ComplaintDto dto = new ComplaintDto(
                complaint.getId(),
                complaint.getResident() != null ? complaint.getResident().getId() : null,
                roomNumber,
                complaint.getCategory(),
                title,
                desc,
                complaint.getImageUrl(),
                complaint.getStatus(),
                complaint.getCreatedDate()
        );
        dto.setResidentName(complaint.getResident() != null ? complaint.getResident().getName() : "Resident");
        dto.setSubmissionType(complaint.getSubmissionType() != null ? complaint.getSubmissionType() : "COMPLAINT");
        dto.setPermissionCategory(complaint.getPermissionCategory());
        dto.setStartDate(complaint.getStartDate());
        dto.setEndDate(complaint.getEndDate());
        dto.setAdminResponse(complaint.getAdminResponse());
        dto.setHasAttachment(complaint.getImageData() != null && complaint.getImageData().length > 0);
        return dto;
    }
}
