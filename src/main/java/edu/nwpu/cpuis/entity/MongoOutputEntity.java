package edu.nwpu.cpuis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//这个仅仅是输出的信息
/**
 * @author fujiazheng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoOutputEntity {
    private Integer id;
    private List<OtherUser> others;

    //这么写的话，前端刷新的时候，就是dd['id']这样，否则就是dd[0]
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherUser {
        private Integer id;
        private Double similarity;
    }
}
