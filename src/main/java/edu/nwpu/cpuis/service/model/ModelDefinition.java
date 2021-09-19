package edu.nwpu.cpuis.service.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * python源文件目录为python/model/ <br/>
 * 配置文件前缀为 "models.definition"
 *
 * @author fujiazheng
 */
@ConfigurationProperties(prefix = "models")
@Component
@Data
@Validated
@Slf4j
public class ModelDefinition {
    @NotNull
    private Map<String, SingleModel> definition;


    @PostConstruct
    private void setData() {
        for (Map.Entry<String, SingleModel> entry : definition.entrySet ()) {
            final SingleModel model = entry.getValue ();
            if (!StageHolder.data.containsKey (model.getStage ())) {
                StageHolder.data.put (model.getStage (), new ArrayList<> ());
            }
            StageHolder.data.get (model.getStage ()).add (model);
        }
    }

    public List<SingleModel> getByStage(int stage) {
        return StageHolder.data.get (stage);
    }

    public Map<Integer, List<SingleModel>> getAll() {
        return StageHolder.data;
    }

    //奇怪的设计。。
    @Data
    private static class StageHolder {
        private static Map<Integer, List<SingleModel>> data = new HashMap<> ();
    }

    @Data
    public static class SingleModel {
        @NotBlank
        private String name;
        @Pattern(regexp = ".*\\.py")
        private String trainSource;
        @Pattern(regexp = ".*\\.py")
        private String predictSource;
        @NotNull
        private int stage;
        private Map<String, String> attributes;
    }
}
