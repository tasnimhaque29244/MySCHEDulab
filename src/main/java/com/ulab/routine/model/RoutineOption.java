package com.ulab.routine.model;

import java.util.ArrayList;
import java.util.List;

public class RoutineOption {
    private List<CourseOffering> offerings = new ArrayList<>();

    public RoutineOption() {
    }

    public RoutineOption(List<CourseOffering> offerings) {
        this.offerings = offerings;
    }

    public List<CourseOffering> getOfferings() {
        return offerings;
    }

    public void setOfferings(List<CourseOffering> offerings) {
        this.offerings = offerings;
    }
}