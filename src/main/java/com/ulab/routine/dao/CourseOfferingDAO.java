package com.ulab.routine.dao;

import com.ulab.routine.model.CourseOffering;
import com.ulab.routine.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CourseOfferingDAO {
    private final ClassMeetingDAO classMeetingDAO = new ClassMeetingDAO();

    public CourseOffering insert(Connection connection, CourseOffering offering) throws Exception {
        String sql = """
                INSERT INTO course_offering
                (batch_id, course_code, section_code, component_type, room, faculty_code, faculty_full_name, raw_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, offering.getBatchId());
            ps.setString(2, offering.getCourseCode());
            ps.setString(3, offering.getSectionCode());
            ps.setString(4, offering.getComponentType());
            ps.setString(5, offering.getRoom());
            ps.setString(6, offering.getFacultyCode());
            ps.setString(7, offering.getFacultyFullName());
            ps.setString(8, offering.getRawText());
            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    offering.setId(rs.getLong(1));
                }
            }
        }

        return offering;
    }
    public List<String> listDistinctCourseCodesByBatchAndFacultyCode(Long batchId, String facultyCode) throws Exception {
    String sql = """
            SELECT DISTINCT course_code
            FROM course_offering
            WHERE batch_id = ?
              AND (
                    UPPER(REPLACE(COALESCE(faculty_code, ''), ' ', '')) LIKE CONCAT('%', UPPER(REPLACE(?, ' ', '')), '%')
                 OR UPPER(REPLACE(COALESCE(faculty_full_name, ''), ' ', '')) LIKE CONCAT('%', UPPER(REPLACE(?, ' ', '')), '%')
              )
            ORDER BY course_code
            """;

    List<String> courses = new ArrayList<>();

    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setLong(1, batchId);
        ps.setString(2, facultyCode);
        ps.setString(3, facultyCode);

        try (var rs = ps.executeQuery()) {
            while (rs.next()) {
                courses.add(rs.getString("course_code"));
            }
        }
    }

    return courses;
}
    public List<String> listDistinctCourseCodesByBatch(Long batchId) throws Exception {
        String sql = "SELECT DISTINCT course_code FROM course_offering WHERE batch_id = ? ORDER BY course_code";
        List<String> courses = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, batchId);

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    courses.add(rs.getString("course_code"));
                }
            }
        }

        return courses;
    }

    public List<CourseOffering> findByBatchAndCourseCodes(Long batchId, List<String> courseCodes) throws Exception {
        if (courseCodes == null || courseCodes.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT id, batch_id, course_code, section_code, component_type, room, faculty_code, faculty_full_name, raw_text
                FROM course_offering
                WHERE batch_id = ?
                AND course_code IN (
                """);

        for (int i = 0; i < courseCodes.size(); i++) {
            sql.append("?");
            if (i < courseCodes.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") ORDER BY course_code, section_code, component_type");

        List<CourseOffering> offerings = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {

            ps.setLong(1, batchId);
            for (int i = 0; i < courseCodes.size(); i++) {
                ps.setString(i + 2, courseCodes.get(i));
            }

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    CourseOffering offering = new CourseOffering();
                    offering.setId(rs.getLong("id"));
                    offering.setBatchId(rs.getLong("batch_id"));
                    offering.setCourseCode(rs.getString("course_code"));
                    offering.setSectionCode(rs.getString("section_code"));
                    offering.setComponentType(rs.getString("component_type"));
                    offering.setRoom(rs.getString("room"));
                    offering.setFacultyCode(rs.getString("faculty_code"));
                    offering.setFacultyFullName(rs.getString("faculty_full_name"));
                    offering.setRawText(rs.getString("raw_text"));
                    offering.setMeetings(classMeetingDAO.findByOfferingId(connection, offering.getId()));
                    offerings.add(offering);
                }
            }
        }

        return offerings;
    }
    public void updateComponentType(Connection connection, Long offeringId, String componentType) throws Exception {
    String sql = "UPDATE course_offering SET component_type = ? WHERE id = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, componentType);
        ps.setLong(2, offeringId);
        ps.executeUpdate();
    }
}

    public void deleteMeetingsByOfferingId(Connection connection, Long offeringId) throws Exception {
    String sql = "DELETE FROM class_meeting WHERE offering_id = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setLong(1, offeringId);
        ps.executeUpdate();
    }
    }
}