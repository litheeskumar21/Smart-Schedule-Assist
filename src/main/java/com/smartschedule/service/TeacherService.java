package com.smartschedule.service;

import com.smartschedule.dto.TeacherDTO;
import com.smartschedule.exception.DuplicateResourceException;
import com.smartschedule.exception.ResourceNotFoundException;
import com.smartschedule.model.Department;
import com.smartschedule.model.Period;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TeacherAvailability;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.TeacherAvailabilityRepository;
import com.smartschedule.repository.TeacherRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import com.smartschedule.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public TeacherService(TeacherRepository teacherRepository, UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          TimetableEntryRepository timetableEntryRepository,
                          TeacherAvailabilityRepository teacherAvailabilityRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Teacher getTeacherByUser(User user) {
        return teacherRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + user.getUsername()));
    }

    public Teacher getTeacherByTeacherId(String teacherId) {
        return teacherRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found for ID: " + teacherId));
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Teacher createTeacher(TeacherDTO dto) {
        if (teacherRepository.existsByTeacherId(dto.getTeacherId()) || userRepository.existsByUsername(dto.getTeacherId())) {
            throw new DuplicateResourceException("Teacher ID already exists: " + dto.getTeacherId());
        }

        User user = new User();
        user.setUsername(dto.getTeacherId());
        String rawPassword = (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) ? "teacher123" : dto.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ROLE_TEACHER);
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setEnabled(true);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setTeacherId(dto.getTeacherId());
        teacher.setUser(user);
        teacher.setPhone(dto.getPhone());

        if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
            Set<Department> depts = new HashSet<>(departmentRepository.findAllById(dto.getDepartmentIds()));
            teacher.setDepartments(depts);
        } else {
            teacher.setDepartments(new HashSet<>());
        }

        return teacherRepository.save(teacher);
    }

    public Teacher updateTeacher(Long id, TeacherDTO dto) {
        Teacher teacher = getTeacherById(id);
        User user = teacher.getUser();
        if (user != null) {
            user.setFullName(dto.getFullName());
            user.setEmail(dto.getEmail());
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            userRepository.save(user);
        }

        teacher.setPhone(dto.getPhone());
        if (dto.getDepartmentIds() != null) {
            Set<Department> depts = new HashSet<>(departmentRepository.findAllById(dto.getDepartmentIds()));
            teacher.setDepartments(depts);
        }

        return teacherRepository.save(teacher);
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);
        User user = teacher.getUser();
        teacherRepository.delete(teacher);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAvailableTeachers(DayOfWeek day, Period period) {
        List<Teacher> allTeachers = teacherRepository.findAll();
        
        // Find teachers who have an active class at this day and period
        List<TimetableEntry> busyEntries = timetableEntryRepository.findByDayOfWeekAndPeriodAndPublishedTrue(day, period);
        Set<Long> busyTeacherIds = busyEntries.stream()
                .filter(e -> e.getStatus() != EntryStatus.CANCELLED)
                .map(e -> {
                    if (e.getStatus() == EntryStatus.SUBSTITUTE && e.getSubstituteTeacher() != null) {
                        return e.getSubstituteTeacher().getId();
                    }
                    return e.getTeacher() != null ? e.getTeacher().getId() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allTeachers.stream()
                .filter(t -> !busyTeacherIds.contains(t.getId()))
                .filter(t -> {
                    Optional<TeacherAvailability> avail = teacherAvailabilityRepository.findByTeacherAndDayOfWeekAndPeriod(t, day, period);
                    return avail.map(TeacherAvailability::isAvailable).orElse(true);
                })
                .collect(Collectors.toList());
    }

    public List<Teacher> getTeachersByDepartment(Department department) {
        return teacherRepository.findByDepartmentsContaining(department);
    }

    public long countTeachers() {
        return teacherRepository.count();
    }
}
