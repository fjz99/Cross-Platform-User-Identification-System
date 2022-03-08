package site.cpuis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoOutputEntity {

    private String userName;
    private List<OtherUser> others;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherUser {

        private String userName;
        private Double similarity;
    }
}
