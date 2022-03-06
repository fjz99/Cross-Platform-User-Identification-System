package site.cpuis.entity;

import lombok.Data;

import java.util.List;

@Data
public class DatasetEntity {
    private String id;
    private List<PathEntity> path;

    @Data
    public static class PathEntity {
        private String time;
        private List<String> degree;//经纬度
        private String location;
        private String message;
    }
}
