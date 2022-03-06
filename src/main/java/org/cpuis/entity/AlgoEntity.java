package org.cpuis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private String trainSource;//name
    @JsonIgnore
    private String testSource;
    @JsonIgnore
    private String predictSource;
    //    private String validationSource;
    private String description;
    private String stage; //1 2 3
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
}
