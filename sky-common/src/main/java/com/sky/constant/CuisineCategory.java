package com.sky.constant;

public enum CuisineCategory {

    CHINESE(1, "中式"),
    WESTERN(2, "西式"),
    JAPANESE(3, "日料"),
    KOREAN(4, "韩料"),
    FAST_FOOD(5, "快餐");

    private final int code;
    private final String desc;

    CuisineCategory(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
