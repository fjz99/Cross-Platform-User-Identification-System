package site.cpuis.entity.vo;

import lombok.Data;

@Data
public class ModelLocationVO {
    private String[] dataset;//dataset应当支持不输入
    private String algoName;//算法名
    private String phase;
    private Integer id;//model id
}
