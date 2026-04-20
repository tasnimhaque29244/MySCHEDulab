package com.ulab.routine.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExcelHelper {
    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelHelper() {
    }

    public static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    public static boolean isTimeHeaderRow(Row row) {
        String first = cellText(row.getCell(0)).toLowerCase();
        int count = 0;

        short lastCell = row.getLastCellNum();
        if (lastCell < 0) {
            return false;
        }

        for (int c = 1; c < lastCell; c++) {
            String text = cellText(row.getCell(c));
            if (TimeParser.isTimeRange(text)) {
                count++;
            }
        }

        return first.contains("day /time") || first.contains("day/time") || count >= 4;
    }

    public static Map<Integer, TimeRange> extractTimeSlots(Row row) {
        Map<Integer, TimeRange> slots = new LinkedHashMap<>();

        short lastCell = row.getLastCellNum();
        if (lastCell < 0) {
            return slots;
        }

        for (int c = 1; c < lastCell; c++) {
            String text = cellText(row.getCell(c));
            if (TimeParser.isTimeRange(text)) {
                slots.put(c, TimeParser.parseRange(text));
            }
        }
        return slots;
    }
}