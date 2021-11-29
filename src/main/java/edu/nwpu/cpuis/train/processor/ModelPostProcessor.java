package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;

import java.util.List;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;

/**
 * 即模型后置处理器，当完成训练后，会获得Output，这个处理器决定如何处理output
 */
public abstract class ModelPostProcessor {
    protected Output output;
    protected int thisId;
    protected String[] dataset;
    protected String algoName;
    protected String phase;
    protected String directoryPath;//模型的checkpoint文件存储位置位置
    protected String modelInfoKey;

    public final void postProcess(Output output, String algoName, List<String> dataset, String phase, int thisId, String directoryPath) {
        this.output = output;
        this.algoName = algoName;
        this.dataset = dataset.toArray (new String[]{});
        this.phase = phase;
        this.thisId = thisId;
        this.directoryPath = directoryPath;
        this.modelInfoKey = ModelKeyGenerator.generateModelInfoKey (this.dataset, algoName, phase, null, modelInfoPrefix);

        process (output, algoName, dataset, phase, thisId, directoryPath);
    }

    protected abstract void process(Output output, String algoName, List<String> dataset, String phase, int thisId, String directoryPath);
}
