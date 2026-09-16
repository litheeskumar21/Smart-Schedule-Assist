package com.smartschedule.repository;

import com.smartschedule.model.Notification;
import com.smartschedule.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean read);
    long countByUserAndIsRead(User user, boolean read);
}
