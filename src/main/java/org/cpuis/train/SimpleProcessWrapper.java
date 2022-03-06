package org.cpuis.train;

import org.cpuis.entity.MongoOutputEntity;
import org.cpuis.entity.Output;
import org.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@SuppressWarnings("unchecked")
public class SimpleProcessWrapper extends AbstractProcessWrapper {

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

    @Override
    public void start() {
        Arrays.sort (dataset);
        String key = String.format ("%s-%s-%s-%s", algoName, dataset[0], dataset[1], phase);
        log.info ("start job {}", key);
        super.start ();
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

    //首先转换成mongoDB的格式，再异步完成逆置操作
    @Override
    protected void afterScriptDone() {
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

    @Override
    public void removeFromMap() {
        PythonScriptRunner.processes.remove (key);
        PythonScriptRunner.processes.remove (getReversedKey (key));
    }
}
