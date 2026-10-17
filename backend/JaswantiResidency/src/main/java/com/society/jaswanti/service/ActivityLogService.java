package com.society.jaswanti.service;

import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logActivity(User user, String action, String module, String description) {
        String ipAddress = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        ActivityLog log = new ActivityLog(user, action, module, description, ipAddress);
        activityLogRepository.save(log);
    }
}
