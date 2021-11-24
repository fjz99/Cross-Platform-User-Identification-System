package edu.nwpu.cpuis.train;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;
import static edu.nwpu.cpuis.train.PythonScriptRunner.mongoService;

/**
 * 实现了训练历史
 * 应该继承 SimpleProcessWrapper,需要抽出getKey方法才行
 *
 * @see SimpleProcessWrapper
 */
@Slf4j
public class TracedProcessWrapper extends ProcessWrapper {
    private static final MongoService<ModelInfo> modelInfoMongoService = PythonScriptRunner.modelInfoMongoService;
    private volatile double percentage = 0;
    private volatile boolean parseOutput = false;
    private Output output;
    private MongoOutputEntity mongoOutputEntity;
    private Integer thisId;
    private String directoryPath;
    private String modelInfoKey;

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
    public void kill() {
        super.kill ();
        PythonScriptRunner.tracedProcesses.remove (key);
    }

    @Override
    protected void cleanupLastOutput() {
    }

    @Override
    protected Thread getDaemon() {
        return new Thread (() -> {
            String s;
            StringBuilder sb = new StringBuilder ();
            key = String.format ("%s-%s-%s-%d", algoName, Arrays.toString (dataset), phase, thisId);//只是log用的key而已
            try {
                while (state == State.TRAINING) {
                    //没有数据读会阻塞，如果返回null，就是进程结束了
                    if ((s = reader.readLine ()) == null) {
                        if (parseOutput && processOutput (sb.toString ().trim ())) {
                            state = State.SUCCESSFULLY_STOPPED;
                            saveToMongoDB ();
                            log.info ("{} successfully stopped", key);
                        } else {
                            log.error ("{} err shutdown stream", key);
                            state = State.ERROR_STOPPED;
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
                                log.warn ("{} err max percentage is: {}", key, percentage);
                                percentage = 100;
                            } else {
                                log.debug ("{} changed to get output", key);
                            }
                        } else {
                            state = State.ERROR_STOPPED;
                            log.error ("{} err input: {}", key, s);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace ();
                state = State.ERROR_STOPPED;
                PythonScriptRunner.tracedProcesses.remove (key);
                log.error ("{} err stop process :{}", key, e.getMessage ());
            }
        });
    }

    private void saveToMongoDB() {
        //检查mongoCollection
        String key = ModelKeyGenerator.generateKeyWithIncId (dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId);
        if (mongoService.collectionExists (key)) {
            mongoService.deleteCollection (key);
            log.warn ("{} 输出的mongo collection已存在", key);
        }
        mongoService.createCollection (key);
        mongoService.createTextIndex (key, "userName", false);
        mongoService.createIndex (key, "userName", false, true);
        for (Map.Entry<String, Object> stringObjectEntry : ((Map<String, Object>) output.getOutput ()).entrySet ()) {
            final String k = stringObjectEntry.getKey ();
            final List<List<String>> v = (List<List<String>>) stringObjectEntry.getValue ();
//                mongoService.createIndex
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
        key = ModelKeyGenerator.generateKey0 (dataset, algoName, phase, "statistics");
        saveStatisticsToMongoDB (key, output);
        log.info ("{} saved output to mongodb", key);
        PythonScriptRunner.executor.submit (reversedOutput (true));

        //下面保存ModelInfo等
        ModelInfo modelInfo = ModelInfo.builder ()
                .id (thisId)
                .time (LocalDateTime.now ())
                .dataLocation (directoryPath)
                .statisticsCollectionName (key)
                .outputCollectionName (ModelKeyGenerator.generateKeyWithIncId (dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId))
                .reversedOutputCollectionName (getReversedKey (PythonScriptRunner.OUTPUT_TYPE, thisId))
                .algo (algoName)
                .dataset (dataset)
                .build ();
        modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    protected void saveStatisticsToMongoDB(String key, Output output) {
        Map<String, Object> statistics = new HashMap<> ();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
        String format = dateTimeFormatter.format (LocalDateTime.now (ZoneOffset.ofHours (8)));
        statistics.put ("trainTimeStamp", format);
        statistics.put ("time", output.getTime ());
        statistics.put ("relatedId", thisId);
        statistics.putAll (output.getOther ());
        PythonScriptRunner.mapMongoService.insert (statistics, key);
    }

    protected String getReversedKey(String type, Integer thisId) {
        String[] reversedDataset = new String[2];
        if (dataset.length == 0) {
            reversedDataset = dataset;
        } else {
            reversedDataset[0] = dataset[1];
            reversedDataset[1] = dataset[0];
        }
        if (thisId != null)
            if (type.equals ("statistics"))
                return ModelKeyGenerator.generateKeyWithIncId (reversedDataset, algoName, phase, type, thisId) + "TRACED_WRAPPER";
            else return ModelKeyGenerator.generateKeyWithIncId (reversedDataset, algoName, phase, type, thisId);
        else if (type.equals ("statistics"))
            return ModelKeyGenerator.generateKey0 (reversedDataset, algoName, phase, type);
        else
            return ModelKeyGenerator.generateKey (reversedDataset, algoName, phase, type);
    }

    protected Runnable reversedOutput(final boolean saveStatistics2Mongo) {
        return () -> {
            String key = getReversedKey (PythonScriptRunner.OUTPUT_TYPE, thisId);
            //检查mongoCollection
            if (mongoService.collectionExists (key)) {
                mongoService.deleteCollection (key);
                log.warn ("{} 输出的mongo collection已存在", key);
            }
            mongoService.createCollection (key);
            mongoService.createTextIndex (key, "userName", false);
            mongoService.createIndex (key, "userName", false, true);

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
            if (saveStatistics2Mongo) {
                saveStatisticsToMongoDB (getReversedKey ("statistics", null), output);
            }
            log.info ("reversed output {} saved", key);
        };
    }

    protected boolean processOutput(String s) {
        try {
            output = JSON.parseObject (s, Output.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace ();
            log.error ("output parse err " + e.getMessage ());
            return false;
        }
    }
}
