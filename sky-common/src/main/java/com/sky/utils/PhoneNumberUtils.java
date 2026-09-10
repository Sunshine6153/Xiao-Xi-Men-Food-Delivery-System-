package com.sky.utils;

public class PhoneNumberUtils {

    public static boolean isPhoneNumberValid(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() != 11) {
            return false;
        }
        return phoneNumber.matches("^1[3-9]\\d{9}$");
    }
}
