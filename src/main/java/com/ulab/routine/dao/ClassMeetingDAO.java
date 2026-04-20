package com.ulab.routine.dao;

import com.ulab.routine.model.ClassMeeting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClassMeetingDAO {

    public ClassMeeting insert(Connection connection, ClassMeeting meeting) throws Exception {
        String sql = "INSERT INTO class_meeting (offering_id, day_name, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, meeting.getOfferingId());
            ps.setString(2, meeting.getDayName());
            ps.setTime(3, java.sql.Time.valueOf(meeting.getStartTime()));
            ps.setTime(4, java.sql.Time.valueOf(meeting.getEndTime()));
            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    meeting.setId(rs.getLong(1));
                }
            }
        }
        return meeting;
    }

    public List<ClassMeeting> findByOfferingId(Connection connection, Long offeringId) throws Exception {
        String sql = "SELECT id, offering_id, day_name, start_time, end_time FROM class_meeting WHERE offering_id = ?";
        List<ClassMeeting> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, offeringId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassMeeting meeting = new ClassMeeting();
                    meeting.setId(rs.getLong("id"));
                    meeting.setOfferingId(rs.getLong("offering_id"));
                    meeting.setDayName(rs.getString("day_name"));
                    meeting.setStartTime(rs.getTime("start_time").toLocalTime());
                    meeting.setEndTime(rs.getTime("end_time").toLocalTime());
                    list.add(meeting);
                }
            }
        }

        return list;
    }
}