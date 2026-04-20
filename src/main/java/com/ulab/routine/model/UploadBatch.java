package com.ulab.routine.model;

import java.time.LocalDateTime;

public class UploadBatch {
    private Long id;
    private String semesterName;
    private String originalFilename;
    private LocalDateTime uploadedAt;

    public UploadBatch() {
    }

    public UploadBatch(Long id, String semesterName, String originalFilename, LocalDateTime uploadedAt) {
        this.id = id;
        this.semesterName = semesterName;
        this.originalFilename = originalFilename;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}