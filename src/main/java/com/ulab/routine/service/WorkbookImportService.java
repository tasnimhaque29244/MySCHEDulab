package com.ulab.routine.service;

import com.ulab.routine.dao.ClassMeetingDAO;
import com.ulab.routine.dao.CourseOfferingDAO;
import com.ulab.routine.dao.UploadBatchDAO;
import com.ulab.routine.dto.ParsedCellDTO;
import com.ulab.routine.model.ClassMeeting;
import com.ulab.routine.model.CourseOffering;
import com.ulab.routine.model.UploadBatch;
import com.ulab.routine.util.DBConnection;
import com.ulab.routine.util.DayParser;
import com.ulab.routine.util.ExcelHelper;
import com.ulab.routine.util.TimeRange;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkbookImportService {
    private final UploadBatchDAO uploadBatchDAO = new UploadBatchDAO();
    private final CourseOfferingDAO courseOfferingDAO = new CourseOfferingDAO();
    private final ClassMeetingDAO classMeetingDAO = new ClassMeetingDAO();
    private final SpacyClientService spacyClientService = new SpacyClientService();

    public UploadBatch importWorkbook(File file, String originalFilename, String semesterName) throws Exception {
        if (file == null || !file.getName().toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Please upload a valid .xlsx file.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                UploadBatch batch = new UploadBatch();
                batch.setSemesterName(semesterName);
                batch.setOriginalFilename(originalFilename);
                batch.setUploadedAt(LocalDateTime.now());
                uploadBatchDAO.insert(connection, batch);

                Map<String, CourseOffering> offeringCache = new HashMap<>();

                try (Workbook workbook = WorkbookFactory.create(file)) {
                    for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                        Sheet sheet = workbook.getSheetAt(s);
                        parseSheet(connection, sheet, batch.getId(), offeringCache);
                    }
                }

                finalizeOfferingTypes(connection, offeringCache);

                connection.commit();
                return batch;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
    private List<String> resolveEffectiveDays(String rawText, List<String> currentDays) {
    if (rawText == null || currentDays == null || currentDays.isEmpty()) {
        return currentDays;
    }

    String marker = extractTrailingMarker(rawText);

    if (currentDays.size() == 2) {
        if (isSameDayPair(currentDays, "SUN", "TUE")) {
            if (containsMarkerToken(marker, "S")) {
                return List.of("SUN");
            }
            if (containsMarkerToken(marker, "T")) {
                return List.of("TUE");
            }
        }

        if (isSameDayPair(currentDays, "MON", "WED")) {
            if (containsMarkerToken(marker, "M")) {
                return List.of("MON");
            }
            if (containsMarkerToken(marker, "W")) {
                return List.of("WED");
            }
        }

        if (isSameDayPair(currentDays, "THU", "SAT")) {
            if (containsMarkerToken(marker, "THU")) {
                return List.of("THU");
            }
            if (containsMarkerToken(marker, "SAT") || containsMarkerToken(marker, "SATURDAY")) {
                return List.of("SAT");
            }
        }
    }

    return currentDays;
}

private boolean isSameDayPair(List<String> days, String day1, String day2) {
    return days.size() == 2 &&
            ((day1.equals(days.get(0)) && day2.equals(days.get(1))) ||
             (day2.equals(days.get(0)) && day1.equals(days.get(1))));
}

private String extractTrailingMarker(String rawText) {
    Matcher matcher = Pattern.compile("\\(([^()]*)\\)\\s*$").matcher(rawText.trim());
    if (matcher.find()) {
        return matcher.group(1).trim().toUpperCase();
    }
    return "";
}

private boolean containsMarkerToken(String marker, String token) {
    if (marker == null || marker.isBlank()) {
        return false;
    }

    return Pattern.compile("(^|[^A-Z])" + Pattern.quote(token.toUpperCase()) + "($|[^A-Z])")
            .matcher(marker.toUpperCase())
            .find();
}
    private void parseSheet(Connection connection,
                            Sheet sheet,
                            Long batchId,
                            Map<String, CourseOffering> offeringCache) throws Exception {
        boolean inExplicitThuLabBlock = false;
        Map<Integer, TimeRange> currentSlots = new HashMap<>();
        List<String> currentDays = List.of();

        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            String firstCell = ExcelHelper.cellText(row.getCell(0));
            List<String> resolvedDays = DayParser.resolveDays(firstCell);
            
            String normalizedFirstCell = firstCell == null ? "" : firstCell.replaceAll("\\s+", " ").trim().toUpperCase();

            if (normalizedFirstCell.contains("CSE & EEE LAB")) {
                inExplicitThuLabBlock = true;
                currentDays = List.of("THU");
                continue;
            }

            if (!normalizedFirstCell.isBlank()
                    && !normalizedFirstCell.contains("CSE & EEE LAB")
                    && !normalizedFirstCell.contains("ENG LAB")
                    && !normalizedFirstCell.contains("ESK")
                    && !resolvedDays.isEmpty()) {
                inExplicitThuLabBlock = false;
            }
            
            if (!resolvedDays.isEmpty()) {
                currentDays = resolvedDays;
            }

            if (ExcelHelper.isTimeHeaderRow(row)) {
                currentSlots = ExcelHelper.extractTimeSlots(row);
                continue;
            }

            if (currentDays.isEmpty() || currentSlots.isEmpty()) {
                continue;
            }

            List<Integer> slotColumns = new ArrayList<>(currentSlots.keySet());
            slotColumns.sort(Integer::compareTo);

            for (Integer colIndex : slotColumns) {
                TimeRange slot = currentSlots.get(colIndex);
                String raw = ExcelHelper.cellText(row.getCell(colIndex));

                if (raw == null || raw.isBlank()) {
                    continue;
                }

                ParsedCellDTO parsed = spacyClientService.parse(raw);
                if (parsed.courseCode == null || parsed.sectionCode == null) {
                    System.out.println("SKIPPED PARSE: " + raw);
                    continue;
                }

                String normalizedSection = normalizeSectionCode(parsed.sectionCode);

                CourseOffering temp = new CourseOffering();
                temp.setBatchId(batchId);
                temp.setCourseCode(parsed.courseCode);
                temp.setSectionCode(normalizedSection);
//                temp.setComponentType("UNKNOWN");
                temp.setComponentType(inExplicitThuLabBlock ? "LAB" : "UNKNOWN");
                temp.setRoom(parsed.room);
                temp.setFacultyCode(parsed.facultyCode);
                temp.setFacultyFullName(parsed.facultyFullName);
                temp.setRawText(parsed.rawText);

                String key = batchId + "|" + temp.uniqueKey();
                CourseOffering offering = offeringCache.get(key);

                if (offering == null) {
                    offering = courseOfferingDAO.insert(connection, temp);
                    offeringCache.put(key, offering);
                }

                List<String> effectiveDays = resolveEffectiveDays(raw, currentDays);

                for (String day : effectiveDays) {
                    if (!offering.hasMeeting(day, slot.start(), slot.end())) {
                        ClassMeeting meeting = new ClassMeeting();
                        meeting.setOfferingId(offering.getId());
                        meeting.setDayName(day);
                        meeting.setStartTime(slot.start());
                        meeting.setEndTime(slot.end());

                        classMeetingDAO.insert(connection, meeting);
                        offering.addMeeting(meeting);
                    }
                }
                System.out.println("Parsing: " + parsed.courseCode + " - " + parsed.sectionCode);
            }
        }
        
    }

    private void finalizeOfferingTypes(Connection connection,
                                       Map<String, CourseOffering> offeringCache) throws Exception {

        for (CourseOffering offering : offeringCache.values()) {
    boolean isLab = "LAB".equalsIgnoreCase(offering.getComponentType());

    boolean sectionEndsWithL =
            offering.getSectionCode() != null &&
            normalizeSectionCode(offering.getSectionCode()).endsWith("L");

    if (sectionEndsWithL) {
        isLab = true;
    }

    List<ClassMeeting> mergedMeetings = mergeAdjacentMeetings(offering.getMeetings());

    boolean mergedIntoLongerBlock = mergedMeetings.size() < offering.getMeetings().size();
    if (mergedIntoLongerBlock) {
        isLab = true;
    }

    boolean hasTwoHourMeeting = false;
    for (ClassMeeting meeting : mergedMeetings) {
        long minutes = Duration.between(meeting.getStartTime(), meeting.getEndTime()).toMinutes();

        if (minutes >= 110) {
            hasTwoHourMeeting = true;
            break;
        }
    }

    if (hasTwoHourMeeting) {
        isLab = true;
    }

    String componentType = isLab ? "LAB" : "THEORY";
    offering.setComponentType(componentType);
    offering.setMeetings(mergedMeetings);

    courseOfferingDAO.updateComponentType(connection, offering.getId(), componentType);
    replaceMeetings(connection, offering);
}
    }

    private void replaceMeetings(Connection connection, CourseOffering offering) throws Exception {
        courseOfferingDAO.deleteMeetingsByOfferingId(connection, offering.getId());

        for (ClassMeeting meeting : offering.getMeetings()) {
            meeting.setId(null);
            meeting.setOfferingId(offering.getId());
            classMeetingDAO.insert(connection, meeting);
        }
    }

    private List<ClassMeeting> mergeAdjacentMeetings(List<ClassMeeting> meetings) {
        Map<String, List<ClassMeeting>> byDay = new HashMap<>();

        for (ClassMeeting meeting : meetings) {
            byDay.computeIfAbsent(meeting.getDayName(), k -> new ArrayList<>()).add(meeting);
        }

        List<ClassMeeting> merged = new ArrayList<>();

        for (Map.Entry<String, List<ClassMeeting>> entry : byDay.entrySet()) {
            List<ClassMeeting> dayMeetings = entry.getValue();
            dayMeetings.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

            int i = 0;
            while (i < dayMeetings.size()) {
                ClassMeeting current = copyMeeting(dayMeetings.get(i));

                while (i + 1 < dayMeetings.size()) {
                    ClassMeeting next = dayMeetings.get(i + 1);
                    long gap = Duration.between(current.getEndTime(), next.getStartTime()).toMinutes();

                    if (gap >= 0 && gap <= 10) {
                        current.setEndTime(next.getEndTime());
                        i++;
                    } else {
                        break;
                    }
                }

                merged.add(current);
                i++;
            }
        }

        merged.sort((a, b) -> {
            int dayCompare = a.getDayName().compareTo(b.getDayName());
            if (dayCompare != 0) {
                return dayCompare;
            }
            return a.getStartTime().compareTo(b.getStartTime());
        });

        return merged;
    }

    private ClassMeeting copyMeeting(ClassMeeting original) {
        ClassMeeting copy = new ClassMeeting();
        copy.setDayName(original.getDayName());
        copy.setStartTime(original.getStartTime());
        copy.setEndTime(original.getEndTime());
        return copy;
    }

    private String normalizeSectionCode(String sectionCode) {
        if (sectionCode == null) {
            return null;
        }
        return sectionCode.replaceAll("\\s+", "").toUpperCase();
    }
}