package org.cpuis.entity;

import lombok.Data;

import java.util.Map;

@Data
public class Output {
    private String name;
    private String[] dataset;
    private Map<String, Object> other;
    private Object output;
    private Boolean success;
    private long time;//s
}
