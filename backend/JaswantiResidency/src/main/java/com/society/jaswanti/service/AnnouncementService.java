package com.society.jaswanti.service;

import com.society.jaswanti.dto.AnnouncementDto;
import com.society.jaswanti.entity.Announcement;
import com.society.jaswanti.repository.AnnouncementRepository;
import com.society.jaswanti.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SystemNotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               SystemNotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

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
        Announcement saved = announcementRepository.save(announcement);
        try {
            notificationService.createBroadcast("New announcement", saved.getTitle(), null);
        } catch (RuntimeException e) {
            logger.warn("Could not create notification: {}", e.getMessage());
        }
        return convertToDto(saved);
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
