package com.sky.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ValidateCodeUtils {

    public static String generateValidateCode(int length) {
        String code = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            code += random.nextInt(10);
        }
        return code;
    }

    public static String generateValidateCode4String(int length) {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            code.append(str.charAt(random.nextInt(62)));
        }
        return code.toString();
    }
}
