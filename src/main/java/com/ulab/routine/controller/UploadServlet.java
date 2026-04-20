package com.ulab.routine.controller;

import com.ulab.routine.dto.UploadResponseDTO;
import com.ulab.routine.model.UploadBatch;
import com.ulab.routine.service.WorkbookImportService;
import com.ulab.routine.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@WebServlet("/api/uploads")
@MultipartConfig(
    location = "/tmp",
    fileSizeThreshold = 1024 * 1024,
    maxFileSize = 20 * 1024 * 1024L,
    maxRequestSize = 25 * 1024 * 1024L
)
public class UploadServlet extends HttpServlet {
    private final WorkbookImportService workbookImportService = new WorkbookImportService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String semester = req.getParameter("semester");
            Part filePart = req.getPart("file");

            if (semester == null || semester.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "Semester is required."));
                return;
            }

            if (filePart == null || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "File is required."));
                return;
            }

            String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!submitted.toLowerCase().endsWith(".xlsx")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "Only .xlsx files are allowed."));
                return;
            }

            Path tempFile = Files.createTempFile("schedule-", ".xlsx");

            try (InputStream inputStream = filePart.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

                UploadBatch batch = workbookImportService.importWorkbook(
                    tempFile.toFile(),
                    submitted,
                    semester
                );

                JsonUtil.writeJson(
                    resp,
                    new UploadResponseDTO(
                        batch.getId(),
                        batch.getSemesterName(),
                        batch.getOriginalFilename()
                    )
                );
            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }
}