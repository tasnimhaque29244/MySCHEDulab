package com.ulab.routine.dto;

import java.util.ArrayList;
import java.util.List;

public class RoutineResponseDTO {
    public int optionNo;
    public List<Entry> entries = new ArrayList<>();

    public RoutineResponseDTO(int optionNo) {
        this.optionNo = optionNo;
    }

    public static class Entry {
        public String courseCode;
        public String sectionCode;
        public String componentType;
        public String room;
        public String faculty;
        public String day;
        public String start;
        public String end;

        public Entry(String courseCode, String sectionCode, String componentType,
                     String room, String faculty, String day, String start, String end) {
            this.courseCode = courseCode;
            this.sectionCode = sectionCode;
            this.componentType = componentType;
            this.room = room;
            this.faculty = faculty;
            this.day = day;
            this.start = start;
            this.end = end;
        }
    }
}