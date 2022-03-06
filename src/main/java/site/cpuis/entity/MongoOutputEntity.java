package site.cpuis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.TextIndexed;

import java.util.List;

//这个仅仅是输出的信息
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoOutputEntity {

    private String userName;
    private List<OtherUser> others;

    //这么写的话，前端刷新的时候，就是dd['id']这样，否则就是dd[0]
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherUser {

        private String userName;
        private Double similarity;
    }
}
