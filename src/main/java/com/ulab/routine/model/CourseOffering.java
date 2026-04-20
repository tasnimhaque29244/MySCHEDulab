package com.ulab.routine.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CourseOffering {
    private Long id;
    private Long batchId;
    private String courseCode;
    private String sectionCode;
    private String componentType;
    private String room;
    private String facultyCode;
    private String facultyFullName;
    private String rawText;
    private List<ClassMeeting> meetings = new ArrayList<>();

    public CourseOffering() {
    }

    public boolean hasMeeting(String day, LocalTime start, LocalTime end) {
        for (ClassMeeting m : meetings) {
            if (Objects.equals(m.getDayName(), day)
                    && Objects.equals(m.getStartTime(), start)
                    && Objects.equals(m.getEndTime(), end)) {
                return true;
            }
        }
        return false;
    }

    public void addMeeting(ClassMeeting meeting) {
        meetings.add(meeting);
    }

    public String uniqueKey() {
        return safe(courseCode) + "|" + safe(sectionCode) + "|" + safe(room) + "|" + safe(facultyCode);
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getFacultyCode() {
        return facultyCode;
    }

    public void setFacultyCode(String facultyCode) {
        this.facultyCode = facultyCode;
    }

    public String getFacultyFullName() {
        return facultyFullName;
    }

    public void setFacultyFullName(String facultyFullName) {
        this.facultyFullName = facultyFullName;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public List<ClassMeeting> getMeetings() {
        return meetings;
    }

    public void setMeetings(List<ClassMeeting> meetings) {
        this.meetings = meetings;
    }
}