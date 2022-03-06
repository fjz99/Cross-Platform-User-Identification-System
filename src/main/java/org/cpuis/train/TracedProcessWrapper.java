package org.cpuis.train;

import com.alibaba.fastjson.JSON;
import org.cpuis.entity.ErrCode;
import org.cpuis.entity.ModelInfo;
import org.cpuis.entity.exception.CpuisException;
import org.cpuis.service.MongoService;
import org.cpuis.train.output.NoOutputOutput;
import org.cpuis.train.processor.ModelPostProcessor;
import org.cpuis.utils.ModelKeyGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

import static org.cpuis.train.PythonScriptRunner.modelInfoPrefix;
import static org.cpuis.train.PythonScriptRunner.processorFactory;

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
        if (state != State.ERROR_STOPPED) {
            return percentage;
        } else throw new CpuisException (ErrCode.TRAINING_ERROR, reason);
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
            if (outputType == NoOutputOutput.class) {
                log.info ("Skipped parsing {} ", NoOutputOutput.class.getCanonicalName ());
                return true;
            }
            outputData = JSON.parseObject (s, outputType);
            log.info ("parsed output to " + outputType.getName ());
            return true;
        } catch (Exception e) {
            failed ("解析python脚本输出失败: " + e.getMessage ());
            reason = "解析python脚本输出失败: " + e.getMessage ();
            log.error (reason, e);
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
