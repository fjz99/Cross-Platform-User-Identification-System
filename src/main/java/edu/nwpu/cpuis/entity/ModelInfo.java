package edu.nwpu.cpuis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelInfo {
    private Integer id;
    private String outputCollectionName;
    private String reversedOutputCollectionName;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    private String statisticsCollectionName;
    private String dataLocation;//对应文件的路径
    private String algo;
    private String[] dataset;
}
