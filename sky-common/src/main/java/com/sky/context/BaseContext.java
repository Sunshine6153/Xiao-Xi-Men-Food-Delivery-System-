package com.sky.context;

import com.sky.constant.Constant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
