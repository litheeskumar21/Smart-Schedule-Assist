package com.smartschedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.dto.GenerationResult;
import com.smartschedule.model.AbsenceRequest;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.enums.AbsenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen2.5:3b}")
    private String defaultModel;

    public OllamaService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Checks whether local Ollama service is reachable.
     */
    public boolean isOllamaAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(ollamaBaseUrl + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Ollama service not available at {}: {}", ollamaBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves list of locally downloaded models from Ollama.
     */
    public List<String> getAvailableModels() {
        List<String> modelNames = new ArrayList<>();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(ollamaBaseUrl + "/api/tags", String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode models = root.get("models");
                if (models != null && models.isArray()) {
                    for (JsonNode m : models) {
                        JsonNode name = m.get("name");
                        if (name != null) {
                            modelNames.add(name.asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to list Ollama models: {}", e.getMessage());
        }
        return modelNames;
    }

    /**
     * Parses natural language absence request using Ollama if available,
     * otherwise falls back to deterministic rule-based NLP extraction.
     */
    public AbsenceRequestDTO parseAbsencePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return new AbsenceRequestDTO();
        }

        if (isOllamaAvailable()) {
            List<String> models = getAvailableModels();
            String activeModel = selectActiveModel(models);
            if (activeModel != null) {
                try {
                    String systemPrompt = "You are a college timetable scheduling assistant. Extract absence details from user prompt.\n"
                            + "Return strictly a JSON object with keys: 'absenceDate' (YYYY-MM-DD), 'absenceType' ('FULL_DAY' or 'SPECIFIC_PERIODS'), 'periodNumbers' (array of integers, e.g. [1, 2]), 'reason' (string).\n"
                            + "Today's reference date is: " + LocalDate.now() + " (" + LocalDate.now().getDayOfWeek() + ").\n"
                            + "Do not output any explanation, markdown formatting, or text outside the raw JSON object.";

                    Map<String, Object> reqBody = new HashMap<>();
                    reqBody.put("model", activeModel);
                    reqBody.put("prompt", prompt);
                    reqBody.put("system", systemPrompt);
                    reqBody.put("stream", false);
                    reqBody.put("format", "json");

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            ollamaBaseUrl + "/api/generate", entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        String responseText = root.path("response").asText();
                        if (responseText != null && !responseText.isEmpty()) {
                            JsonNode json = objectMapper.readTree(responseText);
                            AbsenceRequestDTO dto = new AbsenceRequestDTO();

                            if (json.has("absenceDate") && !json.get("absenceDate").asText().isEmpty()) {
                                try {
                                    dto.setAbsenceDate(LocalDate.parse(json.get("absenceDate").asText()));
                                } catch (Exception ignored) {}
                            }

                            if (json.has("absenceType")) {
                                String type = json.get("absenceType").asText();
                                if ("SPECIFIC_PERIODS".equalsIgnoreCase(type)) {
                                    dto.setAbsenceType(AbsenceType.SPECIFIC_PERIODS);
                                } else {
                                    dto.setAbsenceType(AbsenceType.FULL_DAY);
                                }
                            }

                            if (json.has("reason")) {
                                dto.setReason(json.get("reason").asText());
                            }

                            if (dto.getAbsenceDate() != null) {
                                log.info("Successfully extracted absence request via Ollama model {}: {}", activeModel, dto);
                                return dto;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Ollama extraction encountered error, falling back to rule-based parser: {}", e.getMessage());
                }
            }
        }

        // Graceful fallback to deterministic rule-based NLP extraction
        return fallbackRuleBasedParse(prompt);
    }

    /**
     * Explains why a substitute replacement was selected.
     */
    public String explainReplacementProposal(Teacher original, Teacher substitute, TimetableEntry entry, String reasonContext) {
        String originalName = original != null && original.getUser() != null ? original.getUser().getFullName() : "Faculty";
        String substituteName = substitute != null && substitute.getUser() != null ? substitute.getUser().getFullName() : "Substitute";
        String subjectName = entry != null && entry.getSubject() != null ? entry.getSubject().getName() : "Academic Class";
        int periodNum = entry != null && entry.getPeriod() != null ? entry.getPeriod().getPeriodNumber() : 1;
        DayOfWeek day = entry != null ? entry.getDayOfWeek() : DayOfWeek.MONDAY;

        String basicExplanation = substituteName + " was recommended as substitute for " + subjectName
                + " (Period " + periodNum + " on " + day + ") because they hold departmental qualification, are free from conflicting class allocations, and "
                + (reasonContext != null ? reasonContext : "maintain a balanced weekly teaching load.");

        if (!isOllamaAvailable()) {
            return basicExplanation;
        }

        try {
            String activeModel = selectActiveModel(getAvailableModels());
            if (activeModel != null) {
                String prompt = "In 2 concise professional sentences, explain to the college administrator why "
                        + substituteName + " was selected to substitute for " + originalName + " in " + subjectName
                        + " on " + day + " Period " + periodNum + ". Key criteria: " + basicExplanation;

                return callSimplePrompt(activeModel, prompt, basicExplanation);
            }
        } catch (Exception e) {
            log.debug("Ollama explanation call failed, using rule-based text: {}", e.getMessage());
        }

        return basicExplanation;
    }

    /**
     * Generates a plain-English explanation of timetable generation/solver results.
     */
    public String explainSolverResults(GenerationResult result) {
        if (result == null) return "No timetable generation details available.";
        int scheduled = result.getClassesScheduled();
        int free = result.getUnassignedPeriods();
        int conflicts = result.getConflicts();

        String base = "Schedule optimization completed: " + scheduled + " classes successfully placed with "
                + conflicts + " conflicts and " + free + " unassigned/free slots remaining. "
                + "All hard constraints (teacher non-overlap, classroom capability, room conflicts) were verified.";

        if (!isOllamaAvailable()) {
            return base;
        }

        try {
            String activeModel = selectActiveModel(getAvailableModels());
            if (activeModel != null) {
                String prompt = "In 2 concise encouraging sentences for a college principal, summarize this timetable generation result: "
                        + scheduled + " classes scheduled, " + free + " free periods, " + conflicts + " conflicts.";
                return callSimplePrompt(activeModel, prompt, base);
            }
        } catch (Exception e) {
            log.debug("Ollama solver summary call failed: {}", e.getMessage());
        }

        return base;
    }

    /**
     * Generates an administrator briefing for a policy violation.
     */
    public String summarizePolicyViolation(AbsenceRequest request, String violationDetails) {
        String teacherName = request.getTeacher() != null && request.getTeacher().getUser() != null
                ? request.getTeacher().getUser().getFullName() : "Faculty Member";

        String base = "Policy Escalation Alert: " + teacherName + " has requested absence on "
                + request.getAbsenceDate() + " which exceeds institutional policy limits (" + violationDetails + "). "
                + "Administrative review and discretionary override are required before substitute teaching assignments can be confirmed.";

        if (!isOllamaAvailable()) {
            return base;
        }

        try {
            String activeModel = selectActiveModel(getAvailableModels());
            if (activeModel != null) {
                String prompt = "In 2 formal sentences for an administrative audit log, summarize this faculty leave policy violation: "
                        + base;
                return callSimplePrompt(activeModel, prompt, base);
            }
        } catch (Exception e) {
            log.debug("Ollama policy summary call failed: {}", e.getMessage());
        }

        return base;
    }

    private String callSimplePrompt(String model, String userPrompt, String fallbackText) {
        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("model", model);
            reqBody.put("prompt", userPrompt);
            reqBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ollamaBaseUrl + "/api/generate", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String answer = root.path("response").asText();
                if (answer != null && !answer.trim().isEmpty()) {
                    return answer.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Ollama call failed: {}", e.getMessage());
        }
        return fallbackText;
    }

    private String selectActiveModel(List<String> availableModels) {
        if (availableModels == null || availableModels.isEmpty()) {
            return null;
        }
        // Prefer configured default model if present
        for (String m : availableModels) {
            if (m.startsWith(defaultModel) || m.equalsIgnoreCase(defaultModel)) {
                return m;
            }
        }
        // Prefer any qwen or llama model
        for (String m : availableModels) {
            if (m.toLowerCase().contains("qwen") || m.toLowerCase().contains("llama")) {
                return m;
            }
        }
        return availableModels.get(0);
    }

    /**
     * Deterministic rule-based NLP extraction when Ollama is offline or uninstalled.
     */
    public AbsenceRequestDTO fallbackRuleBasedParse(String text) {
        AbsenceRequestDTO dto = new AbsenceRequestDTO();
        String lower = text.toLowerCase();

        // 1. Detect Absence Type
        if (lower.contains("period") || lower.contains("hour") || lower.contains("slot")) {
            dto.setAbsenceType(AbsenceType.SPECIFIC_PERIODS);
        } else {
            dto.setAbsenceType(AbsenceType.FULL_DAY);
        }

        // 2. Extract Date
        LocalDate date = extractDateFromText(lower);
        dto.setAbsenceDate(date != null ? date : LocalDate.now().plusDays(1));

        // 3. Extract Reason
        String reason = extractReasonFromText(text);
        dto.setReason(reason != null && !reason.trim().isEmpty() ? reason : "Personal commitment");

        return dto;
    }

    private LocalDate extractDateFromText(String lower) {
        LocalDate today = LocalDate.now();

        if (lower.contains("today")) {
            return today;
        }
        if (lower.contains("tomorrow")) {
            return today.plusDays(1);
        }

        // Days of week
        for (DayOfWeek dow : DayOfWeek.values()) {
            String name = dow.name().toLowerCase();
            if (lower.contains(name)) {
                // Next occurrence of that day of week
                return today.with(TemporalAdjusters.nextOrSame(dow));
            }
        }

        // ISO Date YYYY-MM-DD
        Pattern isoPattern = Pattern.compile("(\\d{4})[\\-/](\\d{1,2})[\\-/](\\d{1,2})");
        Matcher isoMatcher = isoPattern.matcher(lower);
        if (isoMatcher.find()) {
            try {
                int y = Integer.parseInt(isoMatcher.group(1));
                int m = Integer.parseInt(isoMatcher.group(2));
                int d = Integer.parseInt(isoMatcher.group(3));
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        // Format DD-MM-YYYY
        Pattern ddmmyyyyPattern = Pattern.compile("(\\d{1,2})[\\-/](\\d{1,2})[\\-/](\\d{4})");
        Matcher ddmMatcher = ddmmyyyyPattern.matcher(lower);
        if (ddmMatcher.find()) {
            try {
                int d = Integer.parseInt(ddmMatcher.group(1));
                int m = Integer.parseInt(ddmMatcher.group(2));
                int y = Integer.parseInt(ddmMatcher.group(3));
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String extractReasonFromText(String text) {
        Pattern pattern = Pattern.compile("(?:due to|because of|reason:|have a|suffering from|attending|for)\\s+([^.,;\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return text.trim();
    }
}
