package org.cpuis.entity.vo;

import com.alibaba.fastjson.annotation.JSONField;
import org.cpuis.entity.AlgoEntity;
import org.cpuis.entity.DatasetManageEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelVO {
    private Integer id;
    private AlgoEntity algoInfo; //为了不使用join。。。
    private List<DatasetManageEntity> datasetInfo; //size=2
    private Map<?, ?> statistics;
    @JSONField(format = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime time;
}
