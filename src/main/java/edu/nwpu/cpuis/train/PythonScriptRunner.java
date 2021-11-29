package edu.nwpu.cpuis.train;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.DatasetManageEntity;
import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.service.AlgoService;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.train.processor.ProcessorFactory;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
@Slf4j
@SuppressWarnings("rawtypes")
public final class PythonScriptRunner {
    public static final String OUTPUT_TYPE = "output";
    public static final String METADATA_TYPE = "metadata";
    public static final String modelInfoPrefix = "model-traced";
    public static final String directoryLocationBase = "E:/tracedModelOutput/";
    //key
    static final Map<String, SimpleProcessWrapper> processes = new HashMap<> ();
    static final Map<String, TracedProcessWrapper> tracedProcesses = new HashMap<> ();
    private static final String TRAIN_TYPE_NAME = "train";
    private static final String PREDICT_TYPE_NAME = "predict";
    public static MongoService<MongoOutputEntity> mongoService;
    public static MongoService<Map> mapMongoService;
    public static MongoService<ModelInfo> modelInfoMongoService;
    public static ThreadPoolTaskExecutor executor;
    public static MongoService<AlgoEntity> algoEntityMongoService;
    public static MongoService<DatasetManageEntity> datasetManageEntityMongoService;
    static ProcessorFactory processorFactory;
    static AlgoService algoService;

    private PythonScriptRunner(ThreadPoolTaskExecutor executor,
                               MongoService<MongoOutputEntity> mongoService,
                               MongoService<Map> mapMongoService,
                               MongoService<ModelInfo> modelInfoMongoService,
                               MongoService<AlgoEntity> algoEntityMongoService,
                               MongoService<DatasetManageEntity> datasetManageEntityMongoService,
                               ProcessorFactory processorFactory,
                               AlgoService algoService) {
        PythonScriptRunner.mongoService = mongoService;
        PythonScriptRunner.executor = executor;
        PythonScriptRunner.mapMongoService = mapMongoService;
        PythonScriptRunner.modelInfoMongoService = modelInfoMongoService;
        PythonScriptRunner.algoEntityMongoService = algoEntityMongoService;
        PythonScriptRunner.datasetManageEntityMongoService = datasetManageEntityMongoService;
        PythonScriptRunner.processorFactory = processorFactory;
        PythonScriptRunner.algoService = algoService;
    }

    /**
     * @param args 存在dirs=[E:/hou,xx]，是数据集的位置
     */
    public static SimpleProcessWrapper runScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames) {
        try {
            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            Process exec = Runtime.getRuntime ().exec (cmd);
            String[] dataset = datasetNames.toArray (new String[]{});
            String key = ModelKeyGenerator.generateKey (dataset, algoName, "train", null);
            SimpleProcessWrapper wrapper = new SimpleProcessWrapper (exec, algoName, dataset);
            processes.put (key, wrapper);
            wrapper.start ();
            return wrapper;
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }
    }

    //!调用方给出inputDir！
    public static int runTracedScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames, String phase) {
        try {
            String[] dataset = datasetNames.toArray (new String[]{});
            String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
            if (!modelInfoMongoService.collectionExists (modelInfoKey)) {
                modelInfoMongoService.createCollection (modelInfoKey);
            }
            List<ModelInfo> modelInfos = modelInfoMongoService.selectAll (modelInfoKey, ModelInfo.class);
            int thisId = modelInfos
                    .stream ()
                    .map (ModelInfo::getId)
                    .max (Comparator.naturalOrder ())
                    .orElse (-1) + 1;
            String key = ModelKeyGenerator.generateKeyWithIncId (dataset, algoName, phase, null, thisId);
            String path = getDirectoryPath (algoName, dataset, phase, thisId);
            checkDirectory (algoName, dataset, phase, thisId);
            //根据路径生成cmd
            args.put ("outputDir", path);
            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            Process exec = Runtime.getRuntime ().exec (cmd);
            TracedProcessWrapper wrapper = new TracedProcessWrapper (exec, algoName, dataset, phase, thisId, path);
            tracedProcesses.put (key, wrapper);
            wrapper.start ();
            return thisId;
        } catch (IOException e) {
            e.printStackTrace ();
            return -1;
        }
    }


    private static String buildCmd(Map<String, Object> args, String sourceName) throws IOException {
        String path = sourceName;
//        String path = context.getResources ("classpath:/**/" + sourceName)[0].getFile ().getPath ();
        String cmd = String.format ("python %s", path);
        StringBuilder sb = new StringBuilder (cmd);
        //数组不能加空格。。，不支持数组，必须是List！
        args.forEach ((k, v) -> {
            sb.append (" --").append (k).append ('=');
            if (v instanceof List) {
                sb.append ('[');
                ((List<?>) v).forEach (x -> sb.append (x).append (','));
                sb.deleteCharAt (sb.length () - 1);
                sb.append (']');
            } else sb.append (v);
        });
        return sb.toString ();
    }

    private static String checkDirectory(String algoName, String[] dataset, String phase, Integer thisId) throws IOException {
        String directoryPath = getDirectoryPath (algoName, dataset, phase, thisId);
        File file = new File (directoryPath);
        if (file.exists ()) {
            FileUtils.forceDelete (file);
        }
        file.mkdirs ();
        return directoryPath;
    }

    public static String getDirectoryPath(String algoName, String[] dataset, String phase, Integer thisId) {
        Arrays.sort (dataset);//!
        return String.format ("%s%s/%s-%s/%s/%s/", directoryLocationBase, algoName, dataset[0], dataset[1], phase, thisId);
    }

    public static SimpleProcessWrapper getTrainProcess(String key) {
        return processes.getOrDefault (key, null);
    }

    public static TracedProcessWrapper getTracedProcess(String key) {
        return tracedProcesses.getOrDefault (key, null);
    }
}
