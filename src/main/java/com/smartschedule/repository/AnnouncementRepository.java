package com.smartschedule.repository;

import com.smartschedule.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByActiveOrderByCreatedAtDesc(boolean active);
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
