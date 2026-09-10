package com.sky.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testFormatDate() {
        String result = DateUtils.formatDate(java.time.LocalDate.now());
        assertNotNull(result);
    }
}
