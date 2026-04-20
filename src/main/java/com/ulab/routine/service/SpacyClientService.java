//package com.ulab.routine.service;
//
//import com.google.gson.JsonObject;
//import com.ulab.routine.dto.ParsedCellDTO;
//import com.ulab.routine.util.AppConfig;
//import com.ulab.routine.util.JsonUtil;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class SpacyClientService {
//    private final HttpClient client = HttpClient.newHttpClient();
//    private final String spacyUrl = AppConfig.get("spacy.url", "http://127.0.0.1:8000/parse");
//
//    public ParsedCellDTO parse(String rawText) {
//        try {
//            JsonObject req = new JsonObject();
//            req.addProperty("text", rawText);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(spacyUrl))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(req.toString()))
//                    .build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            if (response.statusCode() == 200) {
//                return JsonUtil.gson().fromJson(response.body(), ParsedCellDTO.class);
//            }
//        } catch (Exception ignored) {
//        }
//
//        return fallbackParse(rawText);
//    }
//
//    private ParsedCellDTO fallbackParse(String rawText) {
//        ParsedCellDTO dto = new ParsedCellDTO();
//        dto.rawText = rawText;
//
//        Matcher course = Pattern.compile("([A-Z]{2,5})\\s*(\\d{3,4})").matcher(rawText);
//        if (course.find()) {
//            dto.courseCode = course.group(1) + " " + course.group(2);
//        }
//
//        Matcher section = Pattern.compile("-\\s*([0-9]{1,2}[A-Z]?)\\s*-").matcher(rawText);
//        if (section.find()) {
//            dto.sectionCode = section.group(1).trim();
//        }
//
//        Matcher room = Pattern.compile("-\\s*([A-Z]{1,3}\\s?\\d{2,3}[A-Z]?)\\s*-").matcher(rawText);
//        if (room.find()) {
//            dto.room = room.group(1).replaceAll("\\s+", " ").trim();
//        }
//
//        Matcher faculty = Pattern.compile("-\\s*([^\\-]+?)\\s*$").matcher(rawText);
//        if (faculty.find()) {
//            dto.facultyCode = faculty.group(1).trim();
//        }
//
//        return dto;
//    }
//}
package com.ulab.routine.service;

import com.google.gson.JsonObject;
import com.ulab.routine.dto.ParsedCellDTO;
import com.ulab.routine.util.AppConfig;
import com.ulab.routine.util.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpacyClientService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String spacyUrl = AppConfig.get("spacy.url", "http://127.0.0.1:8000/parse");

    private static final Pattern ROOM_PATTERN =
            Pattern.compile("\\s-\\s*([A-Z]{1,3}\\s?\\d{2,3}[A-Z]?)\\s-");

    // handles: - 1 - ROOM -, - 1L - ROOM -, - 1 L - ROOM -
    private static final Pattern SECTION_BEFORE_ROOM_PATTERN =
            Pattern.compile("-\\s*([0-9]{1,2}\\s*[A-Z]?)\\s*(?=-\\s*[A-Z]{1,3}\\s?\\d{2,3}[A-Z]?\\s*-)");

    private static final Pattern SECTION_FALLBACK_PATTERN =
            Pattern.compile("-\\s*([0-9]{1,2}\\s*[A-Z]?)\\s*-");

    public ParsedCellDTO parse(String rawText) {
        try {
            JsonObject req = new JsonObject();
            req.addProperty("text", rawText);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(spacyUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ParsedCellDTO dto = JsonUtil.gson().fromJson(response.body(), ParsedCellDTO.class);
                if (dto != null && dto.sectionCode != null) {
                    dto.sectionCode = normalizeSectionCode(dto.sectionCode);
                }
                return dto;
            }
        } catch (Exception ignored) {
        }

        return fallbackParse(rawText);
    }

    private ParsedCellDTO fallbackParse(String rawText) {
        ParsedCellDTO dto = new ParsedCellDTO();
        dto.rawText = rawText;
        dto.courseCode = extractCourseCode(rawText);
        dto.sectionCode = normalizeSectionCode(extractSectionCode(rawText));
        dto.room = extractRoom(rawText);
        dto.facultyCode = extractFaculty(rawText);
        dto.facultyFullName = null;
        return dto;
    }

    private String extractCourseCode(String text) {
        Matcher deptMatcher = Pattern.compile("^\\s*([A-Z]{2,5})\\b").matcher(text);
        if (!deptMatcher.find()) {
            return null;
        }

        String dept = deptMatcher.group(1);

        Matcher roomMatcher = ROOM_PATTERN.matcher(text);
        String prefix = text;
        if (roomMatcher.find()) {
            prefix = text.substring(0, roomMatcher.start());
        }

        Matcher numMatcher = Pattern.compile("(?<!\\d)(\\d{4})(?!\\d)").matcher(prefix);
        List<String> nums = new ArrayList<>();
        while (numMatcher.find()) {
            nums.add(numMatcher.group(1));
        }

        if (!nums.isEmpty()) {
            return dept + " " + nums.get(nums.size() - 1);
        }

        Matcher direct = Pattern.compile("^\\s*" + dept + "\\s*/?\\s*(\\d{3,4})").matcher(text);
        if (direct.find()) {
            return dept + " " + direct.group(1);
        }

        return null;
    }

    private String extractSectionCode(String text) {
        Matcher m = SECTION_BEFORE_ROOM_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        m = SECTION_FALLBACK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        return null;
    }

    private String extractRoom(String text) {
        Matcher m = ROOM_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    private String extractFaculty(String text) {
        Matcher m = Pattern.compile("-\\s*([^\\-]+?)\\s*$").matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String normalizeSectionCode(String sectionCode) {
        if (sectionCode == null) {
            return null;
        }
        return sectionCode.replaceAll("\\s+", "").toUpperCase();
    }
}