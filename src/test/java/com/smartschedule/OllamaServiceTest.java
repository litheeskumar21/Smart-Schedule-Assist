package com.smartschedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.service.OllamaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

class OllamaServiceTest {

    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        ollamaService = new OllamaService(new RestTemplateBuilder(), new ObjectMapper());
    }

    @Test
    void testRuleBasedFallback_TomorrowFullDay() {
        String prompt = "I have a high fever tomorrow and cannot take classes all day";
        AbsenceRequestDTO dto = ollamaService.fallbackRuleBasedParse(prompt);

        assertNotNull(dto);
        assertEquals(LocalDate.now().plusDays(1), dto.getAbsenceDate());
        assertEquals(AbsenceType.FULL_DAY, dto.getAbsenceType());
        assertTrue(dto.getReason().toLowerCase().contains("fever") || dto.getReason().toLowerCase().contains("high"));
    }

    @Test
    void testRuleBasedFallback_SpecificPeriods() {
        String prompt = "Attending research symposium on Friday for Period 2 and 3 due to presentation";
        AbsenceRequestDTO dto = ollamaService.fallbackRuleBasedParse(prompt);

        assertNotNull(dto);
        assertEquals(AbsenceType.SPECIFIC_PERIODS, dto.getAbsenceType());
        assertEquals(LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), dto.getAbsenceDate());
        assertTrue(dto.getReason().toLowerCase().contains("presentation") || dto.getReason().toLowerCase().contains("due to"));
    }

    @Test
    void testRuleBasedFallback_ExplicitIsoDate() {
        String prompt = "Medical appointment on 2026-12-15 full day leave required";
        AbsenceRequestDTO dto = ollamaService.fallbackRuleBasedParse(prompt);

        assertNotNull(dto);
        assertEquals(LocalDate.of(2026, 12, 15), dto.getAbsenceDate());
        assertEquals(AbsenceType.FULL_DAY, dto.getAbsenceType());
    }

    @Test
    void testExplanationGeneration_WhenOllamaOffline() {
        // Explanations return deterministic formatted text without throwing exceptions
        String explanation = ollamaService.explainReplacementProposal(null, null, null, "lowest teaching workload");
        assertNotNull(explanation);
        assertTrue(explanation.contains("lowest teaching workload"));
    }
}
