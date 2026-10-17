package com.society.jaswanti.service;

import com.society.jaswanti.dto.ComplaintDto;
import com.society.jaswanti.entity.Complaint;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final String uploadDir = "uploads/complaints/";

    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository, com.society.jaswanti.repository.RoomRepository roomRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    public ComplaintDto createComplaint(Long residentId, String category, String description, MultipartFile file) throws IOException {
        User resident = userRepository.findById(residentId).orElseThrow(() -> new RuntimeException("User not found"));
        
        Complaint complaint = new Complaint(resident, category, description);
        
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("File size exceeds 5MB limit");
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                throw new RuntimeException("Invalid file type. Only images and PDFs are allowed.");
            }
            
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            complaint.setImageUrl(filePath.toString());
        }
        
        Complaint saved = complaintRepository.save(complaint);
        return convertToDto(saved);
    }

    public List<ComplaintDto> getComplaintsByResident(Long residentId) {
        return complaintRepository.findByResidentId(residentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ComplaintDto> getAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public java.util.Optional<ComplaintDto> updateComplaint(Long id, Complaint updatedComplaint) {
        return complaintRepository.findById(id).map(complaint -> {
            if (updatedComplaint.getCategory() != null) complaint.setCategory(updatedComplaint.getCategory());
            if (updatedComplaint.getDescription() != null) complaint.setDescription(updatedComplaint.getDescription());
            return convertToDto(complaintRepository.save(complaint));
        });
    }

    public java.util.Optional<ComplaintDto> updateComplaintStatus(Long id, String status) {
        return complaintRepository.findById(id).map(complaint -> {
            try {
                complaint.setStatus(com.society.jaswanti.entity.ComplaintStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
            return convertToDto(complaintRepository.save(complaint));
        });
    }

    public void deleteComplaint(Long id) {
        complaintRepository.deleteById(id);
    }

    private ComplaintDto convertToDto(Complaint complaint) {
        String roomNumber = null;
        java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(complaint.getResident().getId());
        if (!rooms.isEmpty()) {
            roomNumber = rooms.get(0).getRoomNumber();
        }
        
        String fullDesc = complaint.getDescription();
        String title = "Complaint";
        String desc = fullDesc;
        
        if (fullDesc != null && fullDesc.contains(" - ")) {
            int splitIdx = fullDesc.indexOf(" - ");
            title = fullDesc.substring(0, splitIdx);
            desc = fullDesc.substring(splitIdx + 3);
        }

        return new ComplaintDto(
                complaint.getId(),
                complaint.getResident().getId(),
                roomNumber,
                complaint.getCategory(),
                title,
                desc,
                complaint.getImageUrl(),
                complaint.getStatus(),
                complaint.getCreatedDate()
        );
    }
}
