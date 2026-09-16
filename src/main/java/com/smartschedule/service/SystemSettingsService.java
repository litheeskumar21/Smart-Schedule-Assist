package com.smartschedule.service;

import com.smartschedule.model.Period;
import com.smartschedule.model.SystemSettings;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class SystemSettingsService {
    private final SystemSettingsRepository systemSettingsRepository;
    private final PeriodRepository periodRepository;

    public SystemSettingsService(SystemSettingsRepository systemSettingsRepository,
                                 PeriodRepository periodRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.periodRepository = periodRepository;
    }

    public String getSetting(String key, String defaultValue) {
        return systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(defaultValue);
    }

    public void setSetting(String key, String value) {
        SystemSettings setting = systemSettingsRepository.findBySettingKey(key)
                .orElse(new SystemSettings());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        systemSettingsRepository.save(setting);
    }

    public String getCurrentSemester() {
        return getSetting("current_semester", "3");
    }

    public void setCurrentSemester(String semester) {
        setSetting("current_semester", semester);
    }

    public String getAcademicYear() {
        return getSetting("academic_year", "2026-2027");
    }

    public void setAcademicYear(String year) {
        setSetting("academic_year", year);
    }

    public void updatePeriodTiming(Long periodId, LocalTime startTime, LocalTime endTime) {
        periodRepository.findById(periodId).ifPresent(p -> {
            p.setStartTime(startTime);
            p.setEndTime(endTime);
            periodRepository.save(p);
        });
    }

    public List<Period> getAllPeriods() {
        return periodRepository.findAllByOrderByPeriodNumberAsc();
    }
}
