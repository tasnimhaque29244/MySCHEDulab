package com.ulab.routine.dao;

import com.ulab.routine.model.UploadBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

public class UploadBatchDAO {

    public UploadBatch insert(Connection connection, UploadBatch batch) throws Exception {
        String sql = "INSERT INTO upload_batch (semester_name, original_filename, uploaded_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, batch.getSemesterName());
            ps.setString(2, batch.getOriginalFilename());
            ps.setTimestamp(3, Timestamp.valueOf(batch.getUploadedAt()));
            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    batch.setId(rs.getLong(1));
                }
            }
        }
        return batch;
    }
}