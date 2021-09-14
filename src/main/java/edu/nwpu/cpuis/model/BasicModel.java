package edu.nwpu.cpuis.model;

import edu.nwpu.cpuis.utils.PythonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final ModelDefinition definition;

    public BasicModel(ModelDefinition definition) {
        this.definition = definition;
    }

    @Override
    public boolean train(List<String> files, String name) {
        PythonUtils.ProcessWrapper process = PythonUtils.getTrainProcess (name);
        if (process == null) {
            final ModelDefinition.SingleModel singleModel = definition.getDefinition ().get (name);
            Map<String, String> map = new HashMap<> ();
            map.put ("file", "123.txt");
            PythonUtils.runScript (name, singleModel.getTrainSource (), map);
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
}
