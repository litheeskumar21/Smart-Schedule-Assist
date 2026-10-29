package com.smartschedule.service;

import com.smartschedule.model.Notification;
import com.smartschedule.model.Section;
import com.smartschedule.model.Student;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.NotificationRepository;
import com.smartschedule.repository.StudentRepository;
import com.smartschedule.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository,
                               StudentRepository studentRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(User user, String title, String message, NotificationType type) {
        if (user == null) return null;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        sendSseEvent(user, saved);
        return saved;
    }

    public void notifyStudentsInSection(Section section, String title, String message, NotificationType type) {
        if (section == null) return;
        List<Student> students = studentRepository.findBySection(section);
        for (Student student : students) {
            if (student.getUser() != null) {
                createNotification(student.getUser(), title, message, type);
            }
        }
    }

    public void notifyTeacher(Teacher teacher, String title, String message, NotificationType type) {
        if (teacher != null && teacher.getUser() != null) {
            createNotification(teacher.getUser(), title, message, type);
        }
    }

    public void notifyAdmins(String title, String message, NotificationType type) {
        List<User> admins = userRepository.findByRole(UserRole.ROLE_ADMIN);
        for (User admin : admins) {
            createNotification(admin, title, message, type);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public SseEmitter subscribe(User user) {
        if (user == null) return null;

        SseEmitter emitter = new SseEmitter(1800000L); // 30 mins
        emitters.put(user.getId(), emitter);

        emitter.onCompletion(() -> emitters.remove(user.getId()));
        emitter.onTimeout(() -> emitters.remove(user.getId()));
        emitter.onError((e) -> emitters.remove(user.getId()));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitters.remove(user.getId());
        }

        return emitter;
    }

    public void sendSseEvent(User user, Notification notification) {
        if (user == null) return;
        SseEmitter emitter = emitters.get(user.getId());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("NOTIFICATION")
                        .data(Map.of(
                                "id", notification.getId(),
                                "title", notification.getTitle(),
                                "message", notification.getMessage(),
                                "type", notification.getType().name(),
                                "createdAt", notification.getCreatedAt().toString()
                        )));
            } catch (Exception e) {
                emitters.remove(user.getId());
            }
        }
    }
}
