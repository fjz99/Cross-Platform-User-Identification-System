package site.cpuis.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadVO {
    private List<String> dataset;
    private String algoName;//算法名
    //    private Integer pageSize, pageNum;//可以为空
//    private String phase;//train，test，predict，evaluate
//    private String searchType;//默认是fulltext
//    private String searchText;

//    private Integer k;//以后可能改
//    private String identifier;//username,id

//    private Integer id;//traced model,-1即为最新的，或者说不考虑
}
