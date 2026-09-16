package com.smartschedule.service;

import com.smartschedule.model.ActivityLog;
import com.smartschedule.model.User;
import com.smartschedule.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(String action, String entityType, Long entityId, User user, String details) {
        log(action, entityType, entityId, user, details, null);
    }

    public void log(String action, String entityType, Long entityId, User user, String details, String ipAddress) {
        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setPerformedBy(user);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivityLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivity(int count) {
        Page<ActivityLog> page = activityLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, count));
        return page.getContent();
    }
}
