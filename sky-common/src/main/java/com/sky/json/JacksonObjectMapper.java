package com.sky.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;

public class JacksonObjectMapper extends ObjectMapper {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";

    public JacksonObjectMapper() {
        super();

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern(DATE_FORMAT);
        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern(TIME_FORMAT);

        javaTimeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(dateTimeFormatter)
        );
        javaTimeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(dateTimeFormatter)
        );

        javaTimeModule.addSerializer(
                LocalDate.class,
                new LocalDateSerializer(dateFormatter)
        );
        javaTimeModule.addDeserializer(
                LocalDate.class,
                new LocalDateDeserializer(dateFormatter)
        );

        javaTimeModule.addSerializer(
                LocalTime.class,
                new LocalTimeSerializer(timeFormatter)
        );
        javaTimeModule.addDeserializer(
                LocalTime.class,
                new LocalTimeDeserializer(timeFormatter)
        );

        registerModule(javaTimeModule);

        // 处理 java.util.Date
        setDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT));

        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}