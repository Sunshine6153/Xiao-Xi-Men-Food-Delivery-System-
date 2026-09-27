package com.neu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SkyApplication.class)
class RedisTest {

    private static final String KEY = "sky:test:redis";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldSetAndGetValueFromRedis() {
        String expected = "Redis connection works";

        stringRedisTemplate.opsForValue().set(KEY, expected, Duration.ofMinutes(10));
        String actual = stringRedisTemplate.opsForValue().get(KEY);

        assertEquals(expected, actual);
        stringRedisTemplate.delete(KEY);
    }
}
