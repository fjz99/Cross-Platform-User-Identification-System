package edu.nwpu.cpuis.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PredictVO {
    private List<String> dataset;//dataset应当支持不输入
    private String algoName;//算法名
    private Integer id;//model id
    private String input;//fixme
}
