package com.ulab.routine.controller;

import com.ulab.routine.dto.GenerateRequestDTO;
import com.ulab.routine.service.RoutineGeneratorService;
import com.ulab.routine.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/routines")
public class RoutineServlet extends HttpServlet {
    private final RoutineGeneratorService routineGeneratorService = new RoutineGeneratorService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            GenerateRequestDTO request = JsonUtil.gson().fromJson(req.getReader(), GenerateRequestDTO.class);

            if (request == null || request.batchId == null || request.courseCodes == null || request.courseCodes.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "batchId and courseCodes are required."));
                return;
            }

            JsonUtil.writeJson(
                    resp,
                    routineGeneratorService.generate(
                            request.batchId,
                            request.courseCodes,
                            request.limit,
                            request.noClassesBefore,
                            request.noClassesAfter,
                            request.excludedDays
                    )
            );
        } catch (Exception e) {
            try {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }
}