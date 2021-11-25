package edu.nwpu.cpuis.train;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@SuppressWarnings("unchecked")
public class SimpleProcessWrapper extends ProcessWrapper {
    private volatile double percentage;
    private volatile boolean parseOutput;
    private Output output;
    private MongoOutputEntity mongoOutputEntity;

    public SimpleProcessWrapper(Process process, String algoName, String[] dataset) {
        super (process, algoName, dataset, "train");
        percentage = 0;
        parseOutput = false;
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
    protected void cleanupLastOutput() {
        //删除上次的输出
//            key = ModelKeyGenerator.generateKey (dataset, algoName, phase, METADATA_TYPE);
//            mongoService.deleteCollection (key);

        key = ModelKeyGenerator.generateKey (dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE);
        PythonScriptRunner.mongoService.deleteCollection (key);
        PythonScriptRunner.mongoService.createCollection (key);
        PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);
        PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);

        final String key = getReversedKey (PythonScriptRunner.OUTPUT_TYPE);
        PythonScriptRunner.mongoService.deleteCollection (getReversedKey (PythonScriptRunner.OUTPUT_TYPE));
        PythonScriptRunner.mongoService.createCollection (key);
        PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);
        PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);

//            mongoService.deleteCollection (getReversedKey (METADATA_TYPE));
        log.info ("cleanup output done");
    }

    protected String getReversedKey(String type) {
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
                while (state == State.TRAINING) {
                    if (stopFlag) {
                        state = State.INTERRUPTED;
                        log.info ("{} 被stop杀死", key);
                        return;
                    }
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
//                                    shutDownReason = -1;
                                log.warn ("{} err max percentage is: {}", key, percentage);
                                percentage = 100;
                            } else {
//                                    shutDownReason = 0;
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
                PythonScriptRunner.processes.remove (key);
                log.error ("{} err stop process :{}", key, e.getMessage ());
            }
        });
    }

    //首先转换成mongoDB的格式，再异步完成逆置操作
    private void saveToMongoDB() {
        for (Map.Entry<String, Object> stringObjectEntry : ((Map<String, Object>) output.getOutput ()).entrySet ()) {
            final String k = stringObjectEntry.getKey ();
            final List<List<String>> v = (List<List<String>>) stringObjectEntry.getValue ();
            final String key = ModelKeyGenerator.generateKey (dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE);
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
            PythonScriptRunner.mongoService.insert (mongoOutputEntity, key);
//            log.debug ("{} data is",mongoOutputEntity);
        }
        //存储统计信息
        String key = ModelKeyGenerator.generateKey (dataset, algoName, phase, "statistics");
        saveStatisticsToMongoDB (key, output);
        log.info ("{} saved output to mongodb", key);
        PythonScriptRunner.executor.submit (reversedOutput (true));
    }

    protected void saveStatisticsToMongoDB(String key, Output output) {
        Map<String, Object> statistics = new HashMap<> ();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
        String format = dateTimeFormatter.format (LocalDateTime.now (ZoneOffset.ofHours (8)));
        statistics.put ("trainTimeStamp", format);
        statistics.put ("time", output.getTime ());
        statistics.putAll (output.getOther ());
        PythonScriptRunner.mapMongoService.insert (statistics, key);
    }

    protected Runnable reversedOutput(final boolean saveStatistics2Mongo) {
        return () -> {
            String key = getReversedKey (PythonScriptRunner.OUTPUT_TYPE);
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
                PythonScriptRunner.mongoService.insert (v, key);
            });

            //存储统计信息
            if (saveStatistics2Mongo) {
                saveStatisticsToMongoDB (getReversedKey ("statistics"), output);
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

    @Override
    public void kill() {
        super.kill ();
        PythonScriptRunner.processes.remove (key);
    }
}
