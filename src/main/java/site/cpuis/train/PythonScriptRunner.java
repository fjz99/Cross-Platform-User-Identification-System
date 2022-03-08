package site.cpuis.train;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import site.cpuis.entity.*;
import site.cpuis.entity.exception.CpuisException;
import site.cpuis.service.AlgoService;
import site.cpuis.service.DataBaseService;
import site.cpuis.service.DatasetService;
import site.cpuis.train.processor.ProcessorFactory;
import site.cpuis.utils.ModelKeyGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
@Slf4j
@SuppressWarnings("rawtypes")
public final class PythonScriptRunner {
    public static final String OUTPUT_TYPE = "output";
    public static final String modelInfoPrefix = "model-traced";
    public static final String directoryLocationBase = "E:/tracedModelOutput/";
    //key
    static final Map<String, SimpleProcessWrapper> processes = new HashMap<> ();
    static final Map<String, TracedProcessWrapper> tracedProcesses = new HashMap<> ();
    private static final String PREDICT_TYPE_NAME = "predict";
    public static DataBaseService<MongoOutputEntity> mongoService;
    public static DataBaseService<Map> mapMongoService;
    public static DataBaseService<ModelInfo> modelInfoMongoService;
    public static ThreadPoolTaskExecutor executor;
    public static DataBaseService<AlgoEntity> algoEntityMongoService;
    public static DataBaseService<DatasetManageEntity> datasetManageEntityMongoService;
    public static DataBaseService<ModelTrainingInfo> modelTrainingInfoMongoService;
    static ProcessorFactory processorFactory;
    static AlgoService algoService;
    static ProcessWrapperFactory processWrapperFactory;
    static DatasetService fileUploadService;
    static String algoBase;
    static MeterRegistry registry;
    static String modelTrainingInfoMongoName;
    static ModelTrainingInfoService modelTrainingInfoService;
    static DatasetService datasetService;

    private PythonScriptRunner(ThreadPoolTaskExecutor executor,
                               DataBaseService<MongoOutputEntity> mongoService,
                               DataBaseService<Map> mapMongoService,
                               DataBaseService<ModelInfo> modelInfoMongoService,
                               DataBaseService<AlgoEntity> algoEntityMongoService,
                               DataBaseService<DatasetManageEntity> datasetManageEntityMongoService,
                               ProcessorFactory processorFactory,
                               AlgoService algoService,
                               ProcessWrapperFactory processWrapperFactory, DatasetService fileUploadService,
                               @Value("${file.algo-base-location}") String algoBase,
                               @Value("${modelTrainingInfoMongoName}") String modelTrainingInfoMongoName,
                               MeterRegistry registry,
                               DataBaseService<ModelTrainingInfo> modelTrainingInfoMongoService,
                               ModelTrainingInfoService modelTrainingInfoService,
                               DatasetService datasetService) {
        PythonScriptRunner.mongoService = mongoService;
        PythonScriptRunner.executor = executor;
        PythonScriptRunner.mapMongoService = mapMongoService;
        PythonScriptRunner.modelInfoMongoService = modelInfoMongoService;
        PythonScriptRunner.algoEntityMongoService = algoEntityMongoService;
        PythonScriptRunner.datasetManageEntityMongoService = datasetManageEntityMongoService;
        PythonScriptRunner.processorFactory = processorFactory;
        PythonScriptRunner.algoService = algoService;
        PythonScriptRunner.processWrapperFactory = processWrapperFactory;
        PythonScriptRunner.fileUploadService = fileUploadService;
        PythonScriptRunner.algoBase = algoBase;
        PythonScriptRunner.registry = registry;
        PythonScriptRunner.modelTrainingInfoMongoName = modelTrainingInfoMongoName;
        PythonScriptRunner.modelTrainingInfoMongoService = modelTrainingInfoMongoService;
        PythonScriptRunner.modelTrainingInfoService = modelTrainingInfoService;
        PythonScriptRunner.datasetService = datasetService;
    }

    /**
     * @param args 存在dirs=[E:/hou,xx]，是数据集的位置
     */
    public static SimpleProcessWrapper runScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames) {
        Timer timer = registry.timer ("script.time", "algo", algoName, "phase", "train");
        return timer.record (() -> doRunScript (algoName, sourceName, args, datasetNames));
    }

    private static SimpleProcessWrapper doRunScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames) {
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
    public static TracedScriptOutput runTracedScript(String algoName, String sourceName, Map<String, Object> args,
                                                     List<String> datasetNames, String phase, boolean sync) {
        try {
            String[] dataset = datasetNames.toArray (new String[]{});
            checkAlgoAndDatasetInput (algoName, phase, dataset);
            String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
            if (!modelInfoMongoService.collectionExists (modelInfoKey)) {
                modelInfoMongoService.createCollection (modelInfoKey);
            }
            List<ModelInfo> modelInfos = modelInfoMongoService.selectAll (modelInfoKey, ModelInfo.class);
//            int thisId = modelInfos
//                    .stream ()
//                    .map (ModelInfo::getId)
//                    .max (Comparator.naturalOrder ())
//                    .orElse (-1) + 1;
            int thisId = 0;
            String key = ModelKeyGenerator.generateKeyWithIncId (dataset, algoName, phase, null, thisId);
            String path = getDirectoryPath (algoName, dataset, phase, thisId);
            checkDirectory (algoName, dataset, phase, thisId);
            //根据路径生成cmd
//            args.put ("outputDir", path);
            AlgoEntity algoEntity = algoService.getAlgoEntity (algoName);

            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            ProcessBuilder builder = new ProcessBuilder ();
            builder.command (buildCmdSplit (args, sourceName));
            builder.directory (new File (algoBase, algoName));
            Process process = builder.start ();

            ProcessWrapperFactory.ProcessWrapperInput input = ProcessWrapperFactory
                    .ProcessWrapperInput
                    .builder ()
                    .algoName (algoName)
                    .dataset (dataset)
                    .directoryPath (path)
                    .phase (phase)
                    .process (process)
                    .thisId (thisId)
                    .build ();
            TracedProcessWrapper wrapper = processWrapperFactory.newProcessWrapper (algoEntity.getStage (), phase, input);
            tracedProcesses.put (key, wrapper);
            wrapper.start ();
            TracedScriptOutput output = new TracedScriptOutput ();
            output.setId (thisId);
            if (sync) {
                return wrapper.waitForDone ();
            } else return output;//predict的话，这个id永远为0.。
        } catch (IOException e) {
            //不能e.printStackTrace ();因为这个只会输出到标准输出流，而不会记录在日志中
            log.error ("", e);
            return null;
        }
    }

    private static String[] buildCmdSplit(Map<String, Object> args, String sourceName) {
        List<String> list = new ArrayList<> ();
        list.add ("python");
        list.add (sourceName);
        args.forEach ((k, v) -> {
            StringBuilder sb = new StringBuilder ();
            sb.append ("--").append (k).append ('=');
            if (v instanceof List) {
                sb.append ('[');
                ((List<?>) v).forEach (x -> sb.append (x).append (','));
                sb.deleteCharAt (sb.length () - 1);
                sb.append (']');
            } else sb.append (v);
            list.add (sb.toString ());
        });
        return list.toArray (new String[0]);
    }

    private static String buildCmd(Map<String, Object> args, String sourceName) throws IOException {
        //        String path = context.getResources ("classpath:/**/" + sourceName)[0].getFile ().getPath ();
        String cmd = String.format ("python %s", sourceName);
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

    private static void checkAlgoAndDatasetInput(String algoName, String phase, String... dataset) {
        if (phase.equals (PREDICT_TYPE_NAME)) {
            return;
        }
        AlgoEntity algoEntity = algoService.getAlgoEntity (algoName);
        if (algoEntity == null) {
            throw new CpuisException (ErrCode.ALGO_NOT_EXISTS);
        }
        int max, min;
        String stage = algoEntity.getStage ();
        if (stage.equals ("1")) {
            max = 2;
            min = 2;
        } else if (stage.equals ("2")) {
            max = 1;
            min = 1;
        } else throw new IllegalArgumentException ();
        if (!(dataset.length >= min && dataset.length <= max)) {
            throw new CpuisException (ErrCode.WRONG_DATASET_INPUT, String.format ("need size between [%d,%d] , got %d",
                    min, max, dataset.length));
        }
        for (String s : dataset) {
            if (fileUploadService.getDatasetLocation (s) == null) {
                throw new CpuisException (ErrCode.DATASET_NOT_EXISTS);
            }
        }
    }

    public static String getDirectoryPath(String algoName, String[] dataset, String phase, Integer thisId) {
        Arrays.sort (dataset);//!
        String prefix = String.format ("%s%s/", directoryLocationBase, algoName);
        String postfix = String.format ("/%s/%s/", phase, thisId);
        StringBuilder builder = new StringBuilder (prefix);
        for (String s : dataset) {
            builder.append (s).append ('-');
        }
        builder.deleteCharAt (builder.length () - 1);
        return builder + postfix;
    }

    public static SimpleProcessWrapper getTrainProcess(String key) {
        return processes.getOrDefault (key, null);
    }

    public static TracedProcessWrapper getTracedProcess(String key) {
        return tracedProcesses.getOrDefault (key, null);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TracedScriptOutput {
        private int id;
        private Object output;
        private Class<?> outputType;
    }
}
