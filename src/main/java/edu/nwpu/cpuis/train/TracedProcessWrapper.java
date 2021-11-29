package edu.nwpu.cpuis.train;

import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.train.processor.ModelPostProcessor;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import static edu.nwpu.cpuis.train.PythonScriptRunner.*;

/**
 * 实现了训练历史
 *
 * @see SimpleProcessWrapper
 */
@Slf4j
public class TracedProcessWrapper extends AbstractProcessWrapper {
    private static final MongoService<ModelInfo> modelInfoMongoService = PythonScriptRunner.modelInfoMongoService;
    private final Integer thisId;
    private final String directoryPath;
    private final String modelInfoKey;

    public TracedProcessWrapper(Process process, String algoName, String[] dataset, String phase, Integer thisId, String directoryPath) {
        super (process, algoName, dataset, phase);
        this.thisId = thisId;
        this.directoryPath = directoryPath;
        this.modelInfoKey = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
    }

    @Override
    public void start() {
        log.info ("start job {}-{}", modelInfoKey, thisId);
        super.start ();
    }

    public Output getOutput() {
        if (state == State.SUCCESSFULLY_STOPPED)
            return output;
        else return null;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public void removeFromMap() {
        PythonScriptRunner.tracedProcesses.remove (key);
    }

    @Override
    protected void cleanupLastOutput() {
    }

    @Override
    protected void afterScriptDone() {
        String stage1 = algoService.getAlgoEntity (algoName).getStage ();
        int stage = Integer.parseInt (stage1);
        ModelPostProcessor processor = processorFactory.getProcessor (phase, stage);
        processor.postProcess (output, algoName, Arrays.asList (dataset), phase, thisId, directoryPath);
    }
}
