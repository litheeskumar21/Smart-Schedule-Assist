package com.smartschedule.repository;

import com.smartschedule.model.Announcement;
import com.smartschedule.model.AnnouncementTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementTargetRepository extends JpaRepository<AnnouncementTarget, Long> {
    List<AnnouncementTarget> findByAnnouncement(Announcement announcement);
    void deleteByAnnouncement(Announcement announcement);
}
