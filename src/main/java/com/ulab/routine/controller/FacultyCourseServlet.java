package com.ulab.routine.controller;

import com.ulab.routine.dao.CourseOfferingDAO;
import com.ulab.routine.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/faculty-courses")
public class FacultyCourseServlet extends HttpServlet {
    private final CourseOfferingDAO courseOfferingDAO = new CourseOfferingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String batchIdParam = req.getParameter("batchId");
            String facultyCode = req.getParameter("facultyCode");

            if (batchIdParam == null || batchIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "batchId is required."));
                return;
            }

            if (facultyCode == null || facultyCode.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", "facultyCode is required."));
                return;
            }

            Long batchId = Long.parseLong(batchIdParam.trim());

            JsonUtil.writeJson(
                    resp,
                    courseOfferingDAO.listDistinctCourseCodesByBatchAndFacultyCode(batchId, facultyCode.trim())
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