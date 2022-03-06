package org.cpuis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.cpuis.train.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 没有用{@link ModelInfo}
 * 这个类保存了模型训练信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ModelTrainingInfo {
    private String id;
    private volatile String trainingTime;//pretty
    private volatile State state;
    private volatile String message;//err时才有效
    private volatile String errStreamOutput;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;//训练的时间
    private AlgoEntity algo;
    private List<DatasetManageEntity> dataset;
    private volatile double percentage;
}
