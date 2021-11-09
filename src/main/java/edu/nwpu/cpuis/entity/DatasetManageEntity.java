package edu.nwpu.cpuis.entity;

import lombok.Data;

@Data
public class DatasetManageEntity {
    private String name;//name不可以重复，会根据name创建一个文件夹，然后在其中放文件
    private String author;
    private String contact;
    private String description;
    private String size;
}
