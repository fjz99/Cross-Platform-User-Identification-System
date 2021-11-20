package edu.nwpu.cpuis.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputSearchVO {
    private String[] dataset;
    private String algoName;//算法名
    private Integer pageSize, pageNum;//可以为空
    //    private Integer[] range;//都是included
//    private Integer id;//只查询某个id
    private String phase;//train，test，predict，evaluate
    private String type;//output、statistics等
    private String searchType;//默认是fulltext
    private String searchText;

    private Integer k;//以后可能改

    private Integer id;//traced model,-1即为最新的，或者说不考虑
}
