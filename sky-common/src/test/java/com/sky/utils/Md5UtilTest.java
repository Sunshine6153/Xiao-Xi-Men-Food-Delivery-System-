package com.sky.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Md5UtilTest {

    @Test
    void testGetMd5() {
        String md5 = Md5Util.getMd5("test");
        assertNotNull(md5);
        assertEquals(32, md5.length());
    }
}
