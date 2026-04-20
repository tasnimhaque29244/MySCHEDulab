package com.ulab.routine.service;

import com.ulab.routine.dao.CourseOfferingDAO;
import com.ulab.routine.dto.RoutineResponseDTO;
import com.ulab.routine.model.ClassMeeting;
import com.ulab.routine.model.CourseOffering;
import com.ulab.routine.model.RoutineOption;
import com.ulab.routine.util.AppConfig;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RoutineGeneratorService {
    private final CourseOfferingDAO courseOfferingDAO = new CourseOfferingDAO();

    public List<RoutineResponseDTO> generate(Long batchId,
                                             List<String> selectedCourses,
                                             Integer limitValue,
                                             String noClassesBefore,
                                             String noClassesAfter,
                                             List<String> excludedDays) throws Exception {
        if (batchId == null || selectedCourses == null || selectedCourses.isEmpty()) {
            return List.of();
        }

        int limit = limitValue == null
                ? AppConfig.getInt("app.maxRoutineResults", 100)
                : limitValue;

        LocalTime earliestAllowedStart = parseTimeOrNull(noClassesBefore);
        LocalTime latestAllowedEnd = parseTimeOrNull(noClassesAfter);
        Set<String> blockedDays = normalizeDays(excludedDays);

        List<CourseOffering> offerings = courseOfferingDAO.findByBatchAndCourseCodes(batchId, selectedCourses)
                .stream()
                .filter(offering -> matchesPreferences(offering, earliestAllowedStart, latestAllowedEnd, blockedDays))
                .collect(Collectors.toList());

        Map<String, List<CourseOffering>> grouped = offerings.stream()
                .collect(Collectors.groupingBy(CourseOffering::getCourseCode, TreeMap::new, Collectors.toList()));

        for (String course : selectedCourses) {
            if (!grouped.containsKey(course) || grouped.get(course).isEmpty()) {
                return List.of();
            }
        }

        List<String> orderedCourses = new ArrayList<>(selectedCourses);
        List<RoutineOption> results = new ArrayList<>();
        backtrack(orderedCourses, grouped, 0, new ArrayList<>(), results, limit);

        return toResponse(results);
    }

    private boolean matchesPreferences(CourseOffering offering,
                                       LocalTime earliestAllowedStart,
                                       LocalTime latestAllowedEnd,
                                       Set<String> blockedDays) {
        for (ClassMeeting meeting : offering.getMeetings()) {
            String day = normalizeDay(meeting.getDayName());

            if (blockedDays.contains(day)) {
                return false;
            }

            if (earliestAllowedStart != null && meeting.getStartTime().isBefore(earliestAllowedStart)) {
                return false;
            }

            if (latestAllowedEnd != null && meeting.getEndTime().isAfter(latestAllowedEnd)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> normalizeDays(List<String> days) {
        Set<String> normalized = new HashSet<>();
        if (days == null) {
            return normalized;
        }

        for (String day : days) {
            if (day != null && !day.isBlank()) {
                normalized.add(normalizeDay(day));
            }
        }
        return normalized;
    }

    private String normalizeDay(String day) {
        return day == null ? "" : day.trim().toUpperCase();
    }

    private LocalTime parseTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value.trim());
    }

    private void backtrack(List<String> courses,
                           Map<String, List<CourseOffering>> grouped,
                           int index,
                           List<CourseOffering> current,
                           List<RoutineOption> results,
                           int limit) {
        if (results.size() >= limit) {
            return;
        }

        if (index == courses.size()) {
            results.add(new RoutineOption(new ArrayList<>(current)));
            return;
        }

        String currentCourse = courses.get(index);
        for (CourseOffering candidate : grouped.getOrDefault(currentCourse, List.of())) {
            if (!hasConflict(current, candidate)) {
                current.add(candidate);
                backtrack(courses, grouped, index + 1, current, results, limit);
                current.remove(current.size() - 1);
            }
        }
    }

    private boolean hasConflict(List<CourseOffering> current, CourseOffering candidate) {
        for (CourseOffering existing : current) {
            for (ClassMeeting a : existing.getMeetings()) {
                for (ClassMeeting b : candidate.getMeetings()) {
                    if (a.overlaps(b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<RoutineResponseDTO> toResponse(List<RoutineOption> results) {
        List<RoutineResponseDTO> response = new ArrayList<>();
        Map<String, Integer> dayOrder = new HashMap<>();
        dayOrder.put("SUN", 1);
        dayOrder.put("MON", 2);
        dayOrder.put("TUE", 3);
        dayOrder.put("WED", 4);
        dayOrder.put("THU", 5);
        dayOrder.put("SAT", 6);

        int optionNo = 1;
        for (RoutineOption option : results) {
            RoutineResponseDTO dto = new RoutineResponseDTO(optionNo++);

            for (CourseOffering offering : option.getOfferings()) {
                for (ClassMeeting meeting : offering.getMeetings()) {
                    dto.entries.add(new RoutineResponseDTO.Entry(
                            offering.getCourseCode(),
                            offering.getSectionCode(),
                            offering.getComponentType(),
                            offering.getRoom(),
                            offering.getFacultyCode(),
                            meeting.getDayName(),
                            meeting.getStartTime().toString(),
                            meeting.getEndTime().toString()
                    ));
                }
            }

            dto.entries.sort(Comparator
                    .comparing((RoutineResponseDTO.Entry e) -> dayOrder.getOrDefault(e.day, 99))
                    .thenComparing(e -> e.start)
                    .thenComparing(e -> e.courseCode));

            response.add(dto);
        }

        return response;
    }
}