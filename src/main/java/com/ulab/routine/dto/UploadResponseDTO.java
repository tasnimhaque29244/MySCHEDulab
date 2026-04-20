package com.ulab.routine.dto;

public class UploadResponseDTO {
    public Long batchId;
    public String semesterName;
    public String originalFilename;

    public UploadResponseDTO(Long batchId, String semesterName, String originalFilename) {
        this.batchId = batchId;
        this.semesterName = semesterName;
        this.originalFilename = originalFilename;
    }
}