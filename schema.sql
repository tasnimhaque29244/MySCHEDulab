CREATE DATABASE IF NOT EXISTS ulab_routine
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE ulab_routine;

CREATE TABLE IF NOT EXISTS upload_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_name VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    uploaded_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS course_offering (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    section_code VARCHAR(20) NOT NULL,
    component_type VARCHAR(20) NOT NULL,
    room VARCHAR(50),
    faculty_code VARCHAR(100),
    faculty_full_name VARCHAR(150),
    raw_text VARCHAR(600),
    CONSTRAINT fk_course_offering_batch
        FOREIGN KEY (batch_id) REFERENCES upload_batch(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_offering_batch_course
ON course_offering(batch_id, course_code);

CREATE TABLE IF NOT EXISTS class_meeting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    day_name VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_class_meeting_offering
        FOREIGN KEY (offering_id) REFERENCES course_offering(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_class_meeting_offering
ON class_meeting(offering_id);