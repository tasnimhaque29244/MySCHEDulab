package com.ulab.routine.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonUtil {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(GSON.toJson(data));
    }
}