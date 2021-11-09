package edu.nwpu.cpuis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储到mongoDB的算法信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlgoEntity {
    private String name;//name不可以重复，会根据name创建一个文件夹，然后在其中放文件
    private String author;
    private String contact;
    private String trainSource;//name
    private String testSource;
    private String predictSource;
    //    private String validationSource;
    private String description;
    private String stage; //1 2 3
    private LocalDateTime time;
}
