package com.smartschedule.service;

import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.dto.PolicyValidationResult;
import com.smartschedule.dto.ReplacementPlanDTO;
import com.smartschedule.exception.InvalidOperationException;
import com.smartschedule.exception.ResourceNotFoundException;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.ClassResolution;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AbsenceService {
    private final AbsenceRequestRepository absenceRequestRepository;
    private final AbsenceAffectedClassRepository absenceAffectedClassRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherRepository teacherRepository;
    private final PeriodRepository periodRepository;
    private final NotificationService notificationService;
    private final AbsencePolicyService absencePolicyService;
    private final ReplacementProposalService replacementProposalService;
    private final SubstituteService substituteService;
    private final OllamaService ollamaService;

    public AbsenceService(AbsenceRequestRepository absenceRequestRepository,
                          AbsenceAffectedClassRepository absenceAffectedClassRepository,
                          TimetableEntryRepository timetableEntryRepository,
                          TeacherRepository teacherRepository,
                          PeriodRepository periodRepository,
                          NotificationService notificationService,
                          AbsencePolicyService absencePolicyService,
                          ReplacementProposalService replacementProposalService,
                          @Lazy SubstituteService substituteService,
                          OllamaService ollamaService) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.absenceAffectedClassRepository = absenceAffectedClassRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.teacherRepository = teacherRepository;
        this.periodRepository = periodRepository;
        this.notificationService = notificationService;
        this.absencePolicyService = absencePolicyService;
        this.replacementProposalService = replacementProposalService;
        this.substituteService = substituteService;
        this.ollamaService = ollamaService;
    }

    public synchronized AbsenceRequest submitAbsence(AbsenceRequestDTO dto, Teacher teacher) {
        // 1. Policy & Duplicate Validation
        PolicyValidationResult policyResult = absencePolicyService.validateAbsenceRequest(
                teacher, dto.getAbsenceDate(), dto.getAbsenceType(), dto.getPeriodIds(), null);

        if (!policyResult.isValid()) {
            throw new InvalidOperationException(policyResult.getMessage());
        }

        AbsenceRequest request = new AbsenceRequest();
        request.setTeacher(teacher);
        request.setAbsenceDate(dto.getAbsenceDate());
        request.setAbsenceType(dto.getAbsenceType());
        request.setReason(dto.getReason());
        request.setStatus(RequestStatus.PENDING);

        request.setWeeklyRequestCount(policyResult.getWeeklyRequestCount());
        request.setMonthlyLeaveDays(policyResult.getMonthlyLeaveDays());

        if (policyResult.isViolated()) {
            request.setPolicyViolated(true);
            request.setPolicyViolationReason(policyResult.getMessage());
            request.setEscalatedToAdmin(true);
        } else {
            request.setPolicyViolated(false);
            request.setEscalatedToAdmin(false);
        }

        if (dto.getSuggestedSubstituteId() != null) {
            teacherRepository.findById(dto.getSuggestedSubstituteId())
                    .ifPresent(request::setSuggestedSubstitute);
        }

        AbsenceRequest savedRequest = absenceRequestRepository.save(request);

        // 2. Identify affected classes
        DayOfWeek dayOfWeek = dto.getAbsenceDate().getDayOfWeek();
        List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndDayOfWeekAndPublished(teacher, dayOfWeek, true);

        List<AbsenceAffectedClass> affectedList = new ArrayList<>();
        for (TimetableEntry entry : entries) {
            boolean isAffected = false;
            if (dto.getAbsenceType() == AbsenceType.FULL_DAY) {
                isAffected = true;
            } else if (dto.getPeriodIds() != null && dto.getPeriodIds().contains(entry.getPeriod().getId())) {
                isAffected = true;
            }

            if (isAffected) {
                AbsenceAffectedClass affectedClass = new AbsenceAffectedClass();
                affectedClass.setAbsenceRequest(savedRequest);
                affectedClass.setTimetableEntry(entry);
                affectedClass.setPeriod(entry.getPeriod());
                affectedClass.setResolution(ClassResolution.PENDING);
                affectedList.add(affectedClass);
            }
        }

        absenceAffectedClassRepository.saveAll(affectedList);
        savedRequest.setAffectedClasses(affectedList);

        // 3. Automatically calculate constraint-verified substitute replacements
        if (!affectedList.isEmpty()) {
            ReplacementPlanDTO plan = replacementProposalService.calculateReplacementPlan(savedRequest);
            if (plan.getAiSummary() != null && !plan.getAiSummary().isEmpty()) {
                savedRequest.setAiExplanation(plan.getAiSummary());
            }
        }

        // 4. Notifications
        String teacherName = teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : "Faculty";
        String absenceTypeStr = dto.getAbsenceType() != null ? dto.getAbsenceType().name().replace('_', ' ') : "Absence";
        String reasonStr = dto.getReason() != null && !dto.getReason().trim().isEmpty() ? " Reason: " + dto.getReason() : "";

        if (savedRequest.isPolicyViolated()) {
            String aiSummary = ollamaService.summarizePolicyViolation(savedRequest, policyResult.getMessage());
            savedRequest.setAiExplanation(aiSummary);
            absenceRequestRepository.save(savedRequest);

            notificationService.notifyAdmins(
                    "⚠️ ESCALATION: Absence Policy Violation - " + teacherName,
                    teacherName + " requested " + absenceTypeStr + " on " + dto.getAbsenceDate()
                            + " violating policy: " + policyResult.getMessage() + "." + reasonStr,
                    NotificationType.SYSTEM
            );
        } else {
            notificationService.notifyAdmins(
                    "New Absence Request: " + teacherName,
                    teacherName + " requested " + absenceTypeStr + " on " + dto.getAbsenceDate() + "." + reasonStr,
                    NotificationType.SYSTEM
            );
        }

        return savedRequest;
    }

    public AbsenceRequest approveAbsence(Long requestId, String comment, User admin) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Request has already been " + request.getStatus());
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setAdminComment(comment);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());

        if (request.isPolicyViolated()) {
            request.setAdminOverride(true);
        }

        AbsenceRequest saved = absenceRequestRepository.save(request);

        // Notify teacher
        notificationService.notifyTeacher(request.getTeacher(),
                "Absence Request Approved",
                "Your absence request for " + request.getAbsenceDate() + " has been approved by admin.",
                NotificationType.ABSENCE_APPROVED);

        return saved;
    }

    public AbsenceRequest rejectAbsence(Long requestId, String comment, User admin) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Request has already been " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setAdminComment(comment);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        AbsenceRequest saved = absenceRequestRepository.save(request);

        // Notify teacher with rejection comment
        String msg = "Your absence request for " + request.getAbsenceDate() + " was rejected.";
        if (comment != null && !comment.trim().isEmpty()) {
            msg += " Reason: " + comment;
        }
        notificationService.notifyTeacher(request.getTeacher(), "Absence Request Rejected", msg, NotificationType.ABSENCE_REJECTED);

        return saved;
    }

    public void withdrawAbsence(Long requestId, Teacher teacher) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found"));

        if (!request.getTeacher().getId().equals(teacher.getId())) {
            throw new InvalidOperationException("You can only withdraw your own absence requests.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Cannot withdraw a request that is already " + request.getStatus());
        }

        request.setStatus(RequestStatus.WITHDRAWN);
        absenceRequestRepository.save(request);

        // Notify admins about withdrawal
        String teacherName = teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : "Faculty";
        notificationService.notifyAdmins(
                "Absence Request Withdrawn: " + teacherName,
                teacherName + " withdrew their absence request for " + request.getAbsenceDate() + ".",
                NotificationType.SYSTEM
        );
    }

    public int applyProposedReplacements(Long requestId, User admin) {
        AbsenceRequest request = getAbsenceRequestById(requestId);
        List<AbsenceAffectedClass> affectedClasses = absenceAffectedClassRepository.findByAbsenceRequest(request);

        int appliedCount = 0;
        for (AbsenceAffectedClass ac : affectedClasses) {
            if (ac.getProposedSubstitute() != null && ac.getResolution() == ClassResolution.PENDING) {
                substituteService.assignSubstitute(ac.getId(), ac.getProposedSubstitute().getId(), admin);
                ac.setProposalStatus("RESOLVED");
                absenceAffectedClassRepository.save(ac);
                appliedCount++;
            }
        }
        return appliedCount;
    }

    public ReplacementPlanDTO recalculateReplacementPlan(Long requestId) {
        AbsenceRequest request = getAbsenceRequestById(requestId);
        return replacementProposalService.calculateReplacementPlan(request);
    }

    @Transactional(readOnly = true)
    public List<AbsenceRequest> getPendingRequests() {
        return absenceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<AbsenceRequest> getTeacherAbsences(Teacher teacher) {
        return absenceRequestRepository.findByTeacher(teacher);
    }

    @Transactional(readOnly = true)
    public List<AbsenceRequest> getAllAbsenceRequests() {
        return absenceRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public AbsenceRequest getAbsenceRequestById(Long id) {
        return absenceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<AbsenceAffectedClass> getAffectedClasses(Long requestId) {
        return absenceAffectedClassRepository.findByAbsenceRequestId(requestId);
    }

    public long countPendingAbsences() {
        return absenceRequestRepository.findByStatus(RequestStatus.PENDING).size();
    }
}
