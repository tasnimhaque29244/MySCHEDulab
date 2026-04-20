package com.ulab.routine.dto;

import java.util.List;

public class GenerateRequestDTO {
    public Long batchId;
    public List<String> courseCodes;
    public Integer limit;
    
    public String noClassesBefore;
    public String noClassesAfter;
    public List<String> excludedDays;
}