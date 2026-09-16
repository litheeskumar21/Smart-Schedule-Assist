package com.smartschedule.service;

import com.smartschedule.model.Period;
import com.smartschedule.model.Section;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.SectionRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimetableService {
    private final TimetableEntryRepository timetableEntryRepository;
    private final PeriodRepository periodRepository;
    private final SectionRepository sectionRepository;

    public TimetableService(TimetableEntryRepository timetableEntryRepository,
                            PeriodRepository periodRepository,
                            SectionRepository sectionRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
        this.periodRepository = periodRepository;
        this.sectionRepository = sectionRepository;
    }

    public List<TimetableEntry> getTimetableBySection(Section section) {
        return timetableEntryRepository.findBySectionAndPublishedOrderByDayOfWeekAscPeriod_PeriodNumberAsc(section, true);
    }

    public List<TimetableEntry> getTimetableBySectionAndDay(Section section, DayOfWeek day) {
        return timetableEntryRepository.findBySectionAndDayOfWeekAndPublished(section, day, true);
    }

    public List<TimetableEntry> getTimetableByTeacher(Teacher teacher) {
        return timetableEntryRepository.findByTeacherAndPublishedOrderByDayOfWeekAscPeriod_PeriodNumberAsc(teacher, true);
    }

    public List<TimetableEntry> getTimetableByTeacherAndDay(Teacher teacher, DayOfWeek day) {
        return timetableEntryRepository.findByTeacherAndDayOfWeekAndPublished(teacher, day, true);
    }

    public TimetableEntry getNextClass(Section section) {
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
        LocalTime currentTime = LocalTime.now();

        // Search starting from today, cycling through week days
        List<DayOfWeek> daysToSearch = getWorkingDaysStartingFrom(currentDay);

        for (int i = 0; i < daysToSearch.size(); i++) {
            DayOfWeek day = daysToSearch.get(i);
            boolean isToday = (i == 0 && isWorkingDay(currentDay));

            List<TimetableEntry> entries = timetableEntryRepository.findBySectionAndDayOfWeekAndPublished(section, day, true);
            entries.sort(Comparator.comparingInt(e -> e.getPeriod().getPeriodNumber()));

            for (TimetableEntry entry : entries) {
                if (entry.getPeriod().getType() == PeriodType.CLASS &&
                        entry.getStatus() != EntryStatus.CANCELLED &&
                        entry.getSubject() != null) {

                    if (!isToday || entry.getPeriod().getEndTime().isAfter(currentTime)) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    public TimetableEntry getNextClassForTeacher(Teacher teacher) {
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
        LocalTime currentTime = LocalTime.now();

        List<DayOfWeek> daysToSearch = getWorkingDaysStartingFrom(currentDay);

        for (int i = 0; i < daysToSearch.size(); i++) {
            DayOfWeek day = daysToSearch.get(i);
            boolean isToday = (i == 0 && isWorkingDay(currentDay));

            List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndDayOfWeekAndPublished(teacher, day, true);
            entries.sort(Comparator.comparingInt(e -> e.getPeriod().getPeriodNumber()));

            for (TimetableEntry entry : entries) {
                if (entry.getPeriod().getType() == PeriodType.CLASS &&
                        entry.getStatus() != EntryStatus.CANCELLED &&
                        entry.getSubject() != null) {

                    if (!isToday || entry.getPeriod().getEndTime().isAfter(currentTime)) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    public Map<DayOfWeek, List<TimetableEntry>> getWeeklyTimetable(Section section) {
        List<TimetableEntry> entries = timetableEntryRepository.findBySectionAndPublished(section, true);
        return buildWeeklyMap(entries);
    }

    public Map<DayOfWeek, List<TimetableEntry>> getWeeklyTeacherTimetable(Teacher teacher) {
        List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndPublished(teacher, true);
        return buildWeeklyMap(entries);
    }

    public List<TimetableEntry> getDraftTimetable(Section section) {
        return timetableEntryRepository.findBySectionAndPublishedFalse(section);
    }

    public List<TimetableEntry> getAllEntriesForSection(Section section) {
        return timetableEntryRepository.findBySection(section);
    }

    public TimetableEntry saveTimetableEntry(TimetableEntry entry) {
        return timetableEntryRepository.save(entry);
    }

    public List<TimetableEntry> saveTimetableEntries(List<TimetableEntry> entries) {
        return timetableEntryRepository.saveAll(entries);
    }

    public void publishTimetable(Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElseThrow();
        List<TimetableEntry> allEntries = timetableEntryRepository.findBySection(section);
        for (TimetableEntry entry : allEntries) {
            entry.setPublished(true);
        }
        timetableEntryRepository.saveAll(allEntries);
    }

    public void deleteUnpublishedEntries(Section section) {
        timetableEntryRepository.deleteBySectionAndPublishedFalse(section);
    }

    public List<Period> getAllPeriods() {
        return periodRepository.findAllByOrderByPeriodNumberAsc();
    }

    public List<Period> getClassPeriods() {
        return periodRepository.findByTypeOrderByPeriodNumberAsc(PeriodType.CLASS);
    }

    private Map<DayOfWeek, List<TimetableEntry>> buildWeeklyMap(List<TimetableEntry> entries) {
        Map<DayOfWeek, List<TimetableEntry>> map = new LinkedHashMap<>();
        List<DayOfWeek> workingDays = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );

        for (DayOfWeek day : workingDays) {
            List<TimetableEntry> dayEntries = entries.stream()
                    .filter(e -> e.getDayOfWeek() == day)
                    .sorted(Comparator.comparingInt(e -> e.getPeriod().getPeriodNumber()))
                    .collect(Collectors.toList());
            map.put(day, dayEntries);
        }
        return map;
    }

    private boolean isWorkingDay(DayOfWeek day) {
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private List<DayOfWeek> getWorkingDaysStartingFrom(DayOfWeek startDay) {
        List<DayOfWeek> standardDays = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        if (!isWorkingDay(startDay)) {
            return standardDays;
        }
        List<DayOfWeek> result = new ArrayList<>();
        int startIndex = standardDays.indexOf(startDay);
        for (int i = 0; i < standardDays.size(); i++) {
            result.add(standardDays.get((startIndex + i) % standardDays.size()));
        }
        return result;
    }
}
