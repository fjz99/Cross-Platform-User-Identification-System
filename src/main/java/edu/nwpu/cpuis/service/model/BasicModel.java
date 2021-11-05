package edu.nwpu.cpuis.service.model;

import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.utils.PythonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fujiazheng
 */
@Service
@Data
@Slf4j
public class BasicModel<A, B> implements CheckableModel<A, B> {
    private static final String datasetKey = "dirs";
    private final ModelDefinition definition;
    @Value("${file.input-base-location}")
    private String datasetPath;
    @Resource
    private DatasetService datasetService;

    public BasicModel(ModelDefinition definition) {
        this.definition = definition;
    }

    public static String getDatasetKey() {
        return datasetKey;
    }

    /**
     * @param datasets 文件夹，2个
     * @param name
     * @param args     其他元数据，比如k等
     * @return
     */
    @Override
    public boolean train(List<String> datasets, String name, Map<String, String> args) {
        PythonUtils.ProcessWrapper process = PythonUtils.getTrainProcess (name);
        Assert.isTrue (datasets.size () == 2, "目前只支持2个数据集输入");
        if (process == null) {
            final ModelDefinition.SingleModel singleModel = definition.getDefinition ().get (name);
            Map<String, Object> map = new HashMap<> ();
            List<String> out = new ArrayList<> (datasets.size ());
            for (String s : datasets) {
                out.add (datasetService.getDatasetLocation (s));
            }
            map.put (datasetKey, out);
//            map.putAll (args);//会覆盖
            PythonUtils.runScript (name, singleModel.getTrainSource (), map, datasets);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean load(String name) {
        return false;
    }

    @Override
    public boolean destroy(String name) {
        if (PythonUtils.getTrainProcess (name) == null) {
            log.info ("模型删除失败");
            return false;
        } else {
            PythonUtils.getTrainProcess (name).kill ();
            log.info ("模型删除成功");
            return true;
        }
    }

    @Override
    public B predict(A data, String name) {
        return null;
    }

    @Override
    public Double getPercentage(String name) {
        PythonUtils.ProcessWrapperTrain trainProcess = PythonUtils.getTrainProcess (name);
        if (trainProcess == null) {
            return null;
        }
        return trainProcess.getPercentage ();
    }

    public boolean contains(String name, boolean replaceStopped) {
        if (!replaceStopped)
            return PythonUtils.getTrainProcess (name) != null;
        else
            return PythonUtils.getTrainProcess (name) != null && PythonUtils.getTrainProcess (name).getState () == PythonUtils.State.TRAINING;
    }

    public String getStatus(String name) {
        return PythonUtils.getTrainProcess (name).getState ().toString ();
    }
}
