package edu.nwpu.cpuis.train;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.train.processor.ModelPostProcessor;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;
import static edu.nwpu.cpuis.train.PythonScriptRunner.processorFactory;

/**
 * 实现了训练历史
 *
 * @see SimpleProcessWrapper
 */
@Slf4j
@Getter
public class TracedProcessWrapper extends AbstractProcessWrapper {
    private static final MongoService<ModelInfo> modelInfoMongoService = PythonScriptRunner.modelInfoMongoService;
    private final Integer thisId;
    private final String directoryPath;
    private final String modelInfoKey;
    private final Class<?> outputType;
    private Object outputData;

    public TracedProcessWrapper(Process process, String algoName, String[] dataset, String phase, Integer thisId, String directoryPath, Class<?> outputType) {
        super (process, algoName, dataset, phase);
        this.thisId = thisId;
        this.directoryPath = directoryPath;
        this.outputType = outputType;
        this.modelInfoKey = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
    }

    @Override
    public void start() {
        log.info ("start job {}-{}", modelInfoKey, thisId);
        super.start ();
    }

    public Object getOutputData() {
        if (state == State.SUCCESSFULLY_STOPPED)
            return outputData;
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
        ModelPostProcessor processor = processorFactory.getProcessor (outputType);
        processor.process (this);
    }

    protected boolean processOutput(String s) {
        try {
            outputData = JSON.parseObject (s, outputType);
            log.info ("parsed output to " + outputType.getName ());
            return true;
        } catch (Exception e) {
            e.printStackTrace ();
            log.error ("output parse err " + e.getMessage ());
            return false;
        }
    }

    public PythonScriptRunner.TracedScriptOutput waitForDone() {
        try {
            future.get ();
            return PythonScriptRunner
                    .TracedScriptOutput
                    .builder ()
                    .id (thisId)
                    .output (outputData)
                    .outputType (outputType)
                    .build ();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace ();
            throw new IllegalStateException (e);
        }
    }

}
