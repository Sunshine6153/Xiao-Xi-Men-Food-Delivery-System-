package com.sky.utils;

public class ExcelUtil {

    public static String getExcelContentType(String fileName) {
        if (fileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (fileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return "application/vnd.ms-excel";
    }
}
