package com.society.jaswanti.service;

import com.society.jaswanti.dto.AnnouncementDto;
import com.society.jaswanti.entity.Announcement;
import com.society.jaswanti.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private com.society.jaswanti.repository.UserRepository userRepository;

    public List<AnnouncementDto> getAllActiveAnnouncements() {
        return announcementRepository.findAll().stream()
                .filter(a -> a.getExpiryDate() == null || a.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<AnnouncementDto> getAllAnnouncements() {
        return announcementRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AnnouncementDto createAnnouncement(Announcement announcement, Long adminId) {
        if (adminId != null) {
            userRepository.findById(adminId).ifPresent(announcement::setCreatedBy);
        }
        if (announcement.getCreatedDate() == null) {
            announcement.setCreatedDate(LocalDateTime.now());
        }
        return convertToDto(announcementRepository.save(announcement));
    }

    public java.util.Optional<AnnouncementDto> updateAnnouncement(Long id, Announcement updatedAnnouncement) {
        return announcementRepository.findById(id).map(announcement -> {
            if (updatedAnnouncement.getTitle() != null) announcement.setTitle(updatedAnnouncement.getTitle());
            if (updatedAnnouncement.getDescription() != null) announcement.setDescription(updatedAnnouncement.getDescription());
            if (updatedAnnouncement.getExpiryDate() != null) announcement.setExpiryDate(updatedAnnouncement.getExpiryDate());
            return convertToDto(announcementRepository.save(announcement));
        });
    }

    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    private AnnouncementDto convertToDto(Announcement announcement) {
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getDescription(),
                announcement.getCreatedDate(),
                announcement.getExpiryDate(),
                announcement.getCreatedBy() != null ? announcement.getCreatedBy().getId() : null,
                announcement.getCreatedBy() != null ? announcement.getCreatedBy().getName() : null
        );
    }
}
