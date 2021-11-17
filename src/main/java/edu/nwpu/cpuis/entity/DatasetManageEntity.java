package edu.nwpu.cpuis.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetManageEntity {
    private String name;//name不可以重复，会根据name创建一个文件夹，然后在其中放文件
    private String author;
    private String contact;
    private String description;
    private String size;
    @JSONField(format = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime time;
    private String downloadRelativeURI;//相对路径,磁盘上的
}
