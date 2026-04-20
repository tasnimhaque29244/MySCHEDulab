package com.ulab.routine.controller;

import com.ulab.routine.dao.CourseOfferingDAO;
import com.ulab.routine.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/courses")
public class CourseServlet extends HttpServlet {
    private final CourseOfferingDAO courseOfferingDAO = new CourseOfferingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String batchIdParam = req.getParameter("batchId");
            Long batchId = Long.parseLong(batchIdParam);

            JsonUtil.writeJson(resp, courseOfferingDAO.listDistinctCourseCodesByBatch(batchId));
        } catch (Exception e) {
            try {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJson(resp, java.util.Map.of("error", e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }
}