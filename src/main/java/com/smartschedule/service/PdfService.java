package com.smartschedule.service;

import java.awt.Color;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartschedule.model.AcademicYear;
import com.smartschedule.model.Department;
import com.smartschedule.model.Period;
import com.smartschedule.model.Section;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private static final java.awt.Color PRIMARY_COLOR = new java.awt.Color(26, 115, 232); // #1a73e8
    private static final java.awt.Color HEADER_TEXT_COLOR = java.awt.Color.WHITE;
    private static final java.awt.Color ALT_ROW_COLOR = new java.awt.Color(248, 249, 250);
    private static final java.awt.Color BREAK_BG_COLOR = new java.awt.Color(240, 240, 240);
    private static final java.awt.Color BORDER_COLOR = new java.awt.Color(218, 220, 224);

    private static final List<DayOfWeek> DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    public byte[] generateSectionTimetablePdf(Section section, List<TimetableEntry> entries, List<Period> periods) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
            Paragraph title = new Paragraph("SMART SCHEDULE ASSIST - TIMETABLE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Subtitle
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, java.awt.Color.DARK_GRAY);
            String deptName = section.getDepartment() != null ? section.getDepartment().getName() : "";
            String yearName = section.getAcademicYear() != null ? section.getAcademicYear().getLabel() : "";
            Paragraph subtitle = new Paragraph("Department: " + deptName + " | Year: " + yearName + " | Section: " + section.getDisplayName(), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Table
            PdfPTable table = new PdfPTable(6); // Time + 5 days
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 2f, 2f, 2f, 2f});

            // Table Header
            addHeaderCell(table, "Time / Period");
            for (DayOfWeek day : DAYS) {
                addHeaderCell(table, day.name());
            }

            // Map entries by day and period
            Map<String, TimetableEntry> entryMap = new HashMap<>();
            for (TimetableEntry e : entries) {
                entryMap.put(e.getDayOfWeek().name() + "_" + e.getPeriod().getId(), e);
            }

            // Rows
            boolean alt = false;
            for (Period period : periods) {
                String timeStr = "Period " + period.getPeriodNumber() + "\n" +
                        period.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        period.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

                if (period.getType() != PeriodType.CLASS) {
                    PdfPCell timeCell = createCell(timeStr, true, BREAK_BG_COLOR);
                    table.addCell(timeCell);

                    PdfPCell breakCell = new PdfPCell(new Phrase(period.getType().name(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, java.awt.Color.GRAY)));
                    breakCell.setColspan(5);
                    breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    breakCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    breakCell.setBackgroundColor(BREAK_BG_COLOR);
                    breakCell.setBorderColor(BORDER_COLOR);
                    breakCell.setPadding(6);
                    table.addCell(breakCell);
                } else {
                    java.awt.Color bg = alt ? ALT_ROW_COLOR : java.awt.Color.WHITE;
                    table.addCell(createCell(timeStr, true, bg));

                    for (DayOfWeek day : DAYS) {
                        TimetableEntry e = entryMap.get(day.name() + "_" + period.getId());
                        String cellContent = "FREE";
                        java.awt.Color cellBg = bg;

                        if (e != null && e.getSubject() != null) {
                            String subName = e.getSubject().getName();
                            String teacherName = e.getTeacher() != null ? e.getTeacher().getUser().getFullName() : "";
                            String roomName = e.getClassroom() != null ? e.getClassroom().getName() : "";

                            if (e.getStatus() == EntryStatus.CANCELLED) {
                                cellContent = subName + "\n[CANCELLED]";
                            } else if (e.getStatus() == EntryStatus.SUBSTITUTE && e.getSubstituteTeacher() != null) {
                                cellContent = subName + "\nSub: " + e.getSubstituteTeacher().getUser().getFullName() + "\n" + roomName;
                            } else {
                                cellContent = subName + "\n" + teacherName + "\n" + roomName;
                            }
                        }

                        table.addCell(createCell(cellContent, false, cellBg));
                    }
                    alt = !alt;
                }
            }

            document.add(table);
            document.close();
        } catch (Exception ex) {
            throw new RuntimeException("Error generating Section Timetable PDF", ex);
        }

        return out.toByteArray();
    }

    public byte[] generateTeacherTimetablePdf(Teacher teacher, List<TimetableEntry> entries, List<Period> periods) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
            Paragraph title = new Paragraph("TEACHER SCHEDULE - " + teacher.getUser().getFullName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Subtitle
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, java.awt.Color.DARK_GRAY);
            String depts = teacher.getDepartments().stream().map(Department::getCode).collect(Collectors.joining(", "));
            Paragraph subtitle = new Paragraph("Teacher ID: " + teacher.getTeacherId() + " | Departments: " + depts, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 2f, 2f, 2f, 2f});

            addHeaderCell(table, "Time / Period");
            for (DayOfWeek day : DAYS) {
                addHeaderCell(table, day.name());
            }

            Map<String, TimetableEntry> entryMap = new HashMap<>();
            for (TimetableEntry e : entries) {
                entryMap.put(e.getDayOfWeek().name() + "_" + e.getPeriod().getId(), e);
            }

            boolean alt = false;
            for (Period period : periods) {
                String timeStr = "Period " + period.getPeriodNumber() + "\n" +
                        period.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        period.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

                if (period.getType() != PeriodType.CLASS) {
                    table.addCell(createCell(timeStr, true, BREAK_BG_COLOR));

                    PdfPCell breakCell = new PdfPCell(new Phrase(period.getType().name(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, java.awt.Color.GRAY)));
                    breakCell.setColspan(5);
                    breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    breakCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    breakCell.setBackgroundColor(BREAK_BG_COLOR);
                    breakCell.setBorderColor(BORDER_COLOR);
                    breakCell.setPadding(6);
                    table.addCell(breakCell);
                } else {
                    java.awt.Color bg = alt ? ALT_ROW_COLOR : java.awt.Color.WHITE;
                    table.addCell(createCell(timeStr, true, bg));

                    for (DayOfWeek day : DAYS) {
                        TimetableEntry e = entryMap.get(day.name() + "_" + period.getId());
                        String cellContent = "FREE";

                        if (e != null && e.getSubject() != null) {
                            String sec = e.getSection() != null ? e.getSection().getDisplayName() : "";
                            String subName = e.getSubject().getName();
                            String roomName = e.getClassroom() != null ? e.getClassroom().getName() : "";
                            cellContent = sec + "\n" + subName + "\n" + roomName;
                        }

                        table.addCell(createCell(cellContent, false, bg));
                    }
                    alt = !alt;
                }
            }

            document.add(table);
            document.close();
        } catch (Exception ex) {
            throw new RuntimeException("Error generating Teacher Timetable PDF", ex);
        }

        return out.toByteArray();
    }

    public byte[] generateDepartmentTimetablePdf(Department dept, AcademicYear year, List<Section> sections,
                                                Map<Long, List<TimetableEntry>> sectionEntriesMap, List<Period> periods) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            boolean first = true;
            for (Section section : sections) {
                if (!first) {
                    document.newPage();
                }
                first = false;

                // Title
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
                Paragraph title = new Paragraph(dept.getName() + " - " + year.getLabel() + " - " + section.getDisplayName(), titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(12);
                document.add(title);

                List<TimetableEntry> entries = sectionEntriesMap.getOrDefault(section.getId(), List.of());
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.5f, 2f, 2f, 2f, 2f, 2f});

                addHeaderCell(table, "Time / Period");
                for (DayOfWeek day : DAYS) {
                    addHeaderCell(table, day.name());
                }

                Map<String, TimetableEntry> entryMap = new HashMap<>();
                for (TimetableEntry e : entries) {
                    entryMap.put(e.getDayOfWeek().name() + "_" + e.getPeriod().getId(), e);
                }

                boolean alt = false;
                for (Period period : periods) {
                    String timeStr = "Period " + period.getPeriodNumber() + "\n" +
                            period.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                            period.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

                    if (period.getType() != PeriodType.CLASS) {
                        table.addCell(createCell(timeStr, true, BREAK_BG_COLOR));

                        PdfPCell breakCell = new PdfPCell(new Phrase(period.getType().name(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, java.awt.Color.GRAY)));
                        breakCell.setColspan(5);
                        breakCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        breakCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        breakCell.setBackgroundColor(BREAK_BG_COLOR);
                        breakCell.setBorderColor(BORDER_COLOR);
                        breakCell.setPadding(6);
                        table.addCell(breakCell);
                    } else {
                        java.awt.Color bg = alt ? ALT_ROW_COLOR : java.awt.Color.WHITE;
                        table.addCell(createCell(timeStr, true, bg));

                        for (DayOfWeek day : DAYS) {
                            TimetableEntry e = entryMap.get(day.name() + "_" + period.getId());
                            String cellContent = "FREE";
                            if (e != null && e.getSubject() != null) {
                                cellContent = e.getSubject().getName() + "\n" +
                                        (e.getTeacher() != null ? e.getTeacher().getUser().getFullName() : "") + "\n" +
                                        (e.getClassroom() != null ? e.getClassroom().getName() : "");
                            }
                            table.addCell(createCell(cellContent, false, bg));
                        }
                        alt = !alt;
                    }
                }
                document.add(table);
            }

            document.close();
        } catch (Exception ex) {
            throw new RuntimeException("Error generating Department Timetable PDF", ex);
        }

        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_TEXT_COLOR);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, boolean isHeader, java.awt.Color bg) {
        Font font = isHeader ?
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.DARK_GRAY) :
                FontFactory.getFont(FontFactory.HELVETICA, 8, java.awt.Color.BLACK);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }
}
