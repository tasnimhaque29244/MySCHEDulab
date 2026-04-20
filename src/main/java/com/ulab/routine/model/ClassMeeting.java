package com.ulab.routine.model;

import java.time.LocalTime;

public class ClassMeeting {
    private Long id;
    private Long offeringId;
    private String dayName;
    private LocalTime startTime;
    private LocalTime endTime;

    public ClassMeeting() {
    }

    public ClassMeeting(Long id, Long offeringId, String dayName, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.offeringId = offeringId;
        this.dayName = dayName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean overlaps(ClassMeeting other) {
        if (!dayName.equals(other.dayName)) {
            return false;
        }
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public Long getId() {
        return id;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}