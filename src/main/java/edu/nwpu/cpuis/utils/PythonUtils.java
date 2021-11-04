package edu.nwpu.cpuis.utils;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.service.MongoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author fujiazheng
 */
@Component
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public final class PythonUtils implements ApplicationContextAware {
    public static final String OUTPUT_TYPE = "output";
    public static final String METADATA_TYPE = "metadata";
    //key
    private static final Map<String, ProcessWrapperTrain> processes = new HashMap<> ();
    private static final String TRAIN_TYPE_NAME = "train";
    private static final String PREDICT_TYPE_NAME = "predict";
    static MongoService<MongoOutputEntity> mongoService;
    static MongoService<Map> mapMongoService;
    private static ApplicationContext context;
    private static ThreadPoolTaskExecutor executor;


    private PythonUtils(ThreadPoolTaskExecutor executor, MongoService<MongoOutputEntity> mongoService,
                        MongoService<Map> mapMongoService) {
        PythonUtils.mongoService = mongoService;
        PythonUtils.executor = executor;
        PythonUtils.mapMongoService = mapMongoService;
    }

    /**
     * @param algoName
     * @param sourceName
     * @param args         存在dirs=[E:/hou,xx]，是数据集的位置
     * @param datasetNames
     * @return
     */
    public static ProcessWrapperTrain runScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames) {
        try {
            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            Process exec = Runtime.getRuntime ().exec (cmd);
            String[] dataset = datasetNames.toArray (new String[]{});
            String key = ModelKeyGenerator.generateKey (dataset, algoName, "train", null);
            ProcessWrapperTrain wrapper = new ProcessWrapperTrain (exec, algoName, dataset);
            processes.put (key, wrapper);
            return wrapper;
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }
    }


    private static String buildCmd(Map<String, Object> args, String sourceName) throws IOException {
        String path = context.getResources ("classpath:/**/" + sourceName)[0].getFile ().getPath ();
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

    public static ProcessWrapperTrain getTrainProcess(String key) {
        return processes.getOrDefault (key, null);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public enum State {
        UNTRAINED,
        TRAINING,
        ERROR_STOPPED,//任何异常
        SUCCESSFULLY_STOPPED,
        PREDICTING
    }

    //hash-fb-fs-train-output/metadata
    @Slf4j
    public abstract static class ProcessWrapper {

        protected static final String DONE_LITERAL = "done";
        protected final Process process;
        protected final BufferedReader reader;
        protected final Thread daemon;
        protected volatile PythonUtils.State state;//-1 err 0 ok 1 running
        protected String[] dataset;
        protected String phase;
        protected String algoName;
        protected String key;

        public ProcessWrapper(Process process, String algoName, String[] dataset, String phase) {
            this.state = PythonUtils.State.TRAINING;
            this.algoName = algoName;
            this.dataset = dataset;
            this.process = process;
            this.phase = phase;
            reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
            daemon = getDaemon ();
            cleanupLastOutput ();
            executor.submit (daemon);
        }

        protected abstract void cleanupLastOutput();

        protected abstract Thread getDaemon();

        public void kill() {
            process.destroy ();
        }

        public PythonUtils.State getState() {
            return state;
        }
    }

    public static class ProcessWrapperTrain extends ProcessWrapper {
        private volatile double percentage;
        private volatile boolean parseOutput;
        private Output output;
        private MongoOutputEntity mongoOutputEntity;

        public ProcessWrapperTrain(Process process, String algoName, String[] dataset) {
            super (process, algoName, dataset, "train");
            percentage = 0;
            parseOutput = false;
        }

        public Output getOutput() {
            if (state == PythonUtils.State.SUCCESSFULLY_STOPPED)
                return output;
            else return null;
        }

        public double getPercentage() {
            return percentage;
        }

        @Override
        protected void cleanupLastOutput() {
            //删除上次的输出
//            key = ModelKeyGenerator.generateKey (dataset, algoName, phase, METADATA_TYPE);
//            mongoService.deleteCollection (key);

            key = ModelKeyGenerator.generateKey (dataset, algoName, phase, OUTPUT_TYPE);
            mongoService.deleteCollection (key);
            mongoService.createCollection (key);
            mongoService.createIndex (key, "userName", false, true);
            mongoService.createTextIndex (key, "userName", false);

            final String key = getReversedKey (OUTPUT_TYPE);
            mongoService.deleteCollection (getReversedKey (OUTPUT_TYPE));
            mongoService.createCollection (key);
            mongoService.createIndex (key, "userName", false, true);
            mongoService.createTextIndex (key, "userName", false);

//            mongoService.deleteCollection (getReversedKey (METADATA_TYPE));
            log.info ("cleanup output done");
        }

        private String getReversedKey(String type) {
            String[] reversedDataset = new String[2];
            if (dataset.length == 0) {
                reversedDataset = dataset;
            } else {
                reversedDataset[0] = dataset[1];
                reversedDataset[1] = dataset[0];
            }
            return ModelKeyGenerator.generateKey (reversedDataset, algoName, phase, type);
        }

        @Override
        protected Thread getDaemon() {
            return new Thread (() -> {
                String s;
                StringBuilder sb = new StringBuilder ();
                key = String.format ("%s-%s-%s", algoName, Arrays.toString (dataset), phase);
                try {
                    while (state == PythonUtils.State.TRAINING) {
                        //没有数据读会阻塞，如果返回null，就是进程结束了
                        if ((s = reader.readLine ()) == null) {
                            if (parseOutput && processOutput (sb.toString ().trim ())) {
                                state = PythonUtils.State.SUCCESSFULLY_STOPPED;
                                saveToMongoDB ();
                                log.info ("{} successfully stopped", key);
                            } else {
                                log.error ("{} err shutdown stream", key);
                                state = PythonUtils.State.ERROR_STOPPED;
                            }
                            break;
                        }
                        if (parseOutput) {
                            //处理JSON
                            sb.append (s.trim ());
                        } else if (NumberUtils.isCreatable (s)) {
                            percentage = Double.parseDouble (s);
                            log.debug ("{} percentage changed: {}", key, percentage);
                        } else {
                            //不是小数，规定结束符为DONE_LITERAL
                            if (StringUtils.equals (s, DONE_LITERAL)) {
                                parseOutput = true;
                                if (percentage != 100) {
//                                    shutDownReason = -1;
                                    log.warn ("{} err max percentage is: {}", key, percentage);
                                    percentage = 100;
                                } else {
//                                    shutDownReason = 0;
                                    log.debug ("{} changed to get output", key);
                                }
                            } else {
                                state = PythonUtils.State.ERROR_STOPPED;
                                log.error ("{} err input: {}", key, s);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace ();
                    state = PythonUtils.State.ERROR_STOPPED;
                    PythonUtils.processes.remove (key);
                    log.error ("{} err stop process :{}", key, e.getMessage ());
                }
            });
        }

        //首先转换成mongoDB的格式，再异步完成逆置操作
        private void saveToMongoDB() {
            for (Map.Entry<String, Object> stringObjectEntry : ((Map<String, Object>) output.getOutput ()).entrySet ()) {
                final String k = stringObjectEntry.getKey ();
                final List<List<String>> v = (List<List<String>>) stringObjectEntry.getValue ();
                final String key = ModelKeyGenerator.generateKey (dataset, algoName, phase, OUTPUT_TYPE);
                MongoOutputEntity mongoOutputEntity = new MongoOutputEntity ();
                mongoOutputEntity.setUserName (k);
                mongoOutputEntity.setOthers (new ArrayList<> ());
                for (List<String> x : v) {
                    MongoOutputEntity.OtherUser otherUser = MongoOutputEntity.OtherUser.builder ()
                            .userName (x.get (0))
                            .similarity (Double.parseDouble (x.get (1)))
                            .build ();
                    mongoOutputEntity.getOthers ().add (otherUser);
                }
                mongoOutputEntity.getOthers ().sort (Comparator.comparing (MongoOutputEntity.OtherUser::getSimilarity).reversed ());
                mongoService.insert (mongoOutputEntity, key);
//            log.debug ("{} data is",mongoOutputEntity);
            }
            //存储统计信息
            String key = ModelKeyGenerator.generateKey (dataset, algoName, phase, "statistics");
            saveStatisticsToMongoDB (key, output);
            log.info ("{} saved output to mongodb", key);
            executor.submit (reversedOutput ());
        }

        private void saveStatisticsToMongoDB(String key, Output output) {
            Map<String, Object> statistics = new HashMap<> ();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
            String format = dateTimeFormatter.format (LocalDateTime.now (ZoneOffset.ofHours (8)));
            statistics.put ("trainTimeStamp", format);
            statistics.put ("time", output.getTime ());
            statistics.putAll (output.getOther ());
            mapMongoService.insert (statistics, key);
        }

        private Runnable reversedOutput() {
            return () -> {
                String key = getReversedKey (OUTPUT_TYPE);
                Map<String, Object> map = (Map<String, Object>) output.getOutput ();
                Map<String, MongoOutputEntity> result = new HashMap<> ();
                for (Map.Entry<String, Object> stringObjectEntry : map.entrySet ()) {
                    final String k = stringObjectEntry.getKey ();
                    final List<List<String>> v = (List<List<String>>) stringObjectEntry.getValue ();
                    final String thisKey = k;
                    for (List<String> list : v) {
                        String anotherKey = list.get (0);
                        if (!result.containsKey (anotherKey)) {
                            MongoOutputEntity mongoOutputEntity = new MongoOutputEntity ();
                            mongoOutputEntity.setUserName (anotherKey);
                            mongoOutputEntity.setOthers (new ArrayList<> ());
                            result.put (anotherKey, mongoOutputEntity);
                        }
                        MongoOutputEntity.OtherUser build = MongoOutputEntity.OtherUser.builder ()
                                .userName (thisKey).similarity (Double.valueOf (list.get (1))).build ();
                        result.get (anotherKey).getOthers ().add (build);
                    }
                }

                result.forEach ((k, v) -> {
                    v.getOthers ().sort (Comparator.comparing (MongoOutputEntity.OtherUser::getSimilarity).reversed ());
                    mongoService.insert (v, key);
                });

                //存储统计信息
                saveStatisticsToMongoDB (getReversedKey ("statistics"), output);
                log.info ("reversed output {} saved", key);
            };
        }

        private boolean processOutput(String s) {
            try {
                output = JSON.parseObject (s, Output.class);
                return true;
            } catch (Exception e) {
                e.printStackTrace ();
                log.error ("output parse err " + e.getMessage ());
                return false;
            }
        }

        @Override
        public void kill() {
            super.kill ();
            processes.remove (key);
        }
    }

}

//TODO
//    public static class ProcessWrapperPredict extends ProcessWrapper {
//
//        public ProcessWrapperPredict(Process process, String name) {
//            super (process, name);
//        }
//
//        @Override
//        protected Thread getDaemon() {
//            return null;
//        }
//    }
