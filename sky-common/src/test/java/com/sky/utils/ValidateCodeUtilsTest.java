package com.sky.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidateCodeUtilsTest {

    @Test
    void testGenerateValidateCode() {
        String code = ValidateCodeUtils.generateValidateCode(4);
        assertNotNull(code);
        assertEquals(4, code.length());
    }

    @Test
    void testGenerateValidateCode4String() {
        String code = ValidateCodeUtils.generateValidateCode4String(4);
        assertNotNull(code);
        assertEquals(4, code.length());
    }
}
