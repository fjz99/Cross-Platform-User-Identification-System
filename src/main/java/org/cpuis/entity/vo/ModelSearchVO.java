package org.cpuis.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查找历史模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelSearchVO {
    private String[] dataset;//dataset应当支持不输入
    private String algoName;//算法名
//    private Integer id;//-1即为全部,只有dataset和algoName都确定的时候，才能指定id
    private Integer pageSize, pageNum;//可以为空
    private String phase;
}
