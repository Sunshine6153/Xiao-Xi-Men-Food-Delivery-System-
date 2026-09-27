package com.neu.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {

    private Long id;
    private String name;
    private Integer sort;
}
