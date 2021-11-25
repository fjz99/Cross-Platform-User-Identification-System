package edu.nwpu.cpuis.service.model;

import edu.nwpu.cpuis.service.AlgoService;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.train.ProcessWrapper;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import edu.nwpu.cpuis.train.SimpleProcessWrapper;
import edu.nwpu.cpuis.train.State;
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

@Service
@Data
@Slf4j
public class BasicModel<A, B> {
    private static final String datasetKey = "dirs";
    private final ModelDefinition definition;
    @Value("${file.input-base-location}")
    private String datasetPath;
    @Resource
    private DatasetService datasetService;
    @Resource
    private AlgoService algoService;

    public BasicModel(ModelDefinition definition) {
        this.definition = definition;
    }

    public static String getDatasetKey() {
        return datasetKey;
    }

    /**
     * @param datasets 文件夹，2个
     */
    public boolean train(List<String> datasets, String name, Map<String, String> args) {
        ProcessWrapper process = PythonScriptRunner.getTrainProcess (name);
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
            //fixme
//            PythonUtils.runScript (name, singleModel.getTrainSource (), map, datasets);
            if (algoService.getAlgoEntity (name) == null) {
                return false;
            }
            PythonScriptRunner.runScript (name, algoService.getAlgoEntity (name).getTrainSource (), map, datasets);
            return true;
        } else {
            return false;
        }
    }

    public boolean destroy(String name) {
        if (PythonScriptRunner.getTrainProcess (name) == null) {
            log.info ("模型删除失败");
            return false;
        } else {
            PythonScriptRunner.getTrainProcess (name).kill ();
            log.info ("模型删除成功");
            return true;
        }
    }

    public Double getPercentage(String name) {
        String[] strings = name.split ("-");
        String string = strings[1];
        strings[1] = strings[2];
        strings[2] = string;
        String name2 = String.join ("-", strings);
        SimpleProcessWrapper trainProcess = PythonScriptRunner.getTrainProcess (name);
        SimpleProcessWrapper t2 = PythonScriptRunner.getTrainProcess (name2);
        if (trainProcess == null && t2 == null) {
            return null;
        } else if (t2 != null) {
            return t2.getPercentage ();
        } else return trainProcess.getPercentage ();
    }

    public boolean contains(String name, boolean replaceStopped) {
        if (!replaceStopped)
            return PythonScriptRunner.getTrainProcess (name) != null;
        else
            return PythonScriptRunner.getTrainProcess (name) != null && PythonScriptRunner.getTrainProcess (name).getState () == State.TRAINING;
    }

    public String getStatus(String name) {
        return PythonScriptRunner.getTrainProcess (name).getState ().toString ();
    }
}
