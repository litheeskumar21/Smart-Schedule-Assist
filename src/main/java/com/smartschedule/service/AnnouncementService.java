package com.smartschedule.service;

import com.smartschedule.dto.AnnouncementDTO;
import com.smartschedule.exception.ResourceNotFoundException;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.model.enums.TargetType;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementTargetRepository announcementTargetRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               AnnouncementTargetRepository announcementTargetRepository,
                               StudentRepository studentRepository,
                               TeacherRepository teacherRepository,
                               NotificationService notificationService,
                               UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.announcementTargetRepository = announcementTargetRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public Announcement createAnnouncement(AnnouncementDTO dto, User admin) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setCreatedBy(admin);
        announcement.setTargetType(dto.getTargetType());
        announcement.setScheduledAt(dto.getScheduledAt());
        announcement.setActive(true);

        if (dto.getScheduledAt() == null || dto.getScheduledAt().isBefore(LocalDateTime.now())) {
            announcement.setPublishedAt(LocalDateTime.now());
        }

        Announcement saved = announcementRepository.save(announcement);

        List<AnnouncementTarget> targets = new ArrayList<>();
        if (dto.getTargetIds() != null && !dto.getTargetIds().isEmpty()) {
            for (Long targetId : dto.getTargetIds()) {
                AnnouncementTarget target = new AnnouncementTarget();
                target.setAnnouncement(saved);
                target.setTargetType(dto.getTargetType());
                target.setTargetId(targetId);
                targets.add(target);
            }
            announcementTargetRepository.saveAll(targets);
        }
        saved.setTargets(targets);

        // Broadcast notification if immediately published
        if (saved.getPublishedAt() != null) {
            broadcastAnnouncementNotification(saved);
        }

        return saved;
    }

    public Announcement updateAnnouncement(Long id, AnnouncementDTO dto) {
        Announcement announcement = getAnnouncementById(id);
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setTargetType(dto.getTargetType());
        announcement.setScheduledAt(dto.getScheduledAt());

        announcementTargetRepository.deleteByAnnouncement(announcement);
        List<AnnouncementTarget> targets = new ArrayList<>();
        if (dto.getTargetIds() != null && !dto.getTargetIds().isEmpty()) {
            for (Long targetId : dto.getTargetIds()) {
                AnnouncementTarget target = new AnnouncementTarget();
                target.setAnnouncement(announcement);
                target.setTargetType(dto.getTargetType());
                target.setTargetId(targetId);
                targets.add(target);
            }
            announcementTargetRepository.saveAll(targets);
        }
        announcement.setTargets(targets);

        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(Long id) {
        Announcement announcement = getAnnouncementById(id);
        announcementRepository.delete(announcement);
    }

    @Transactional(readOnly = true)
    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsForUser(User user) {
        if (user == null) return List.of();

        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return getAllAnnouncements();
        }

        List<Announcement> activeAnnouncements = announcementRepository.findByActiveOrderByCreatedAtDesc(true)
                .stream()
                .filter(a -> a.getPublishedAt() != null && a.getPublishedAt().isBefore(LocalDateTime.now().plusMinutes(1)))
                .collect(Collectors.toList());

        if (user.getRole() == UserRole.ROLE_STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            if (studentOpt.isEmpty()) return List.of();
            Student student = studentOpt.get();

            return activeAnnouncements.stream()
                    .filter(a -> isRelevantForStudent(a, student))
                    .collect(Collectors.toList());
        } else if (user.getRole() == UserRole.ROLE_TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findByUser(user);
            if (teacherOpt.isEmpty()) return List.of();
            Teacher teacher = teacherOpt.get();

            return activeAnnouncements.stream()
                    .filter(a -> isRelevantForTeacher(a, teacher))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private boolean isRelevantForStudent(Announcement a, Student student) {
        if (a.getTargetType() == TargetType.EVERYONE || a.getTargetType() == TargetType.STUDENTS) {
            return true;
        }
        if (a.getTargets() == null || a.getTargets().isEmpty()) {
            return true;
        }
        for (AnnouncementTarget target : a.getTargets()) {
            if (target.getTargetType() == TargetType.DEPARTMENT && student.getDepartment() != null &&
                    student.getDepartment().getId().equals(target.getTargetId())) {
                return true;
            }
            if (target.getTargetType() == TargetType.YEAR && student.getAcademicYear() != null &&
                    student.getAcademicYear().getId().equals(target.getTargetId())) {
                return true;
            }
            if (target.getTargetType() == TargetType.SECTION && student.getSection() != null &&
                    student.getSection().getId().equals(target.getTargetId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRelevantForTeacher(Announcement a, Teacher teacher) {
        if (a.getTargetType() == TargetType.EVERYONE || a.getTargetType() == TargetType.TEACHERS) {
            return true;
        }
        if (a.getTargets() == null || a.getTargets().isEmpty()) {
            return true;
        }
        for (AnnouncementTarget target : a.getTargets()) {
            if (target.getTargetType() == TargetType.DEPARTMENT && teacher.getDepartments() != null) {
                boolean matches = teacher.getDepartments().stream()
                        .anyMatch(d -> d.getId().equals(target.getTargetId()));
                if (matches) return true;
            }
        }
        return false;
    }

    private void broadcastAnnouncementNotification(Announcement announcement) {
        List<User> recipients = userRepository.findAll();
        for (User user : recipients) {
            if (user.getRole() != UserRole.ROLE_ADMIN) {
                notificationService.createNotification(user,
                        "New Announcement: " + announcement.getTitle(),
                        announcement.getContent().length() > 100 ?
                                announcement.getContent().substring(0, 100) + "..." : announcement.getContent(),
                        NotificationType.ANNOUNCEMENT);
            }
        }
    }
}
