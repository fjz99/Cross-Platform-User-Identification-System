package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoMongoService;
import static edu.nwpu.cpuis.train.PythonScriptRunner.mongoService;

/**
 * 处理矩阵形式的输出
 * 矩阵为m x m的每个元素是一个二元组(userName,value)
 * <p>
 * 这个是为了兼容算法1
 */
@Slf4j
@Component(ProcessorNames.matrixValuePostProcessor)
public class MatrixValuePostProcessor extends ModelPostProcessor {

    private void saveStatisticsToMongoDB(String key, Output output) {
        Map<String, Object> statistics = new HashMap<> ();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
        String format = dateTimeFormatter.format (LocalDateTime.now (ZoneOffset.ofHours (8)));
        statistics.put ("trainTimeStamp", format);
        statistics.put ("time", output.getTime ());
        statistics.put ("relatedId", thisId);
        statistics.putAll (output.getOther ());
        PythonScriptRunner.mapMongoService.insert (statistics, key);
    }

    private String getReversedKey(String type, Integer thisId) {
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

    private Runnable reversedOutput(final boolean saveStatistics2Mongo) {
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

    @Override
    public void process(Output output, String algoName, List<String> dataset, String phase, int thisId, String directoryPath) {
        super.postProcess (output, algoName, dataset, phase, thisId, directoryPath);

        //检查mongoCollection
        String key = ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId);
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
        key = ModelKeyGenerator.generateKey0 (this.dataset, algoName, phase, "statistics");
        saveStatisticsToMongoDB (key, output);
        log.info ("{} saved output to mongodb", key);
        PythonScriptRunner.executor.submit (reversedOutput (true));

        //下面保存ModelInfo等
        ModelInfo modelInfo = ModelInfo.builder ()
                .id (thisId)
                .time (LocalDateTime.now ())
                .dataLocation (directoryPath)
                .statisticsCollectionName (key)
                .outputCollectionName (ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId))
                .reversedOutputCollectionName (getReversedKey (PythonScriptRunner.OUTPUT_TYPE, thisId))
                .algo (algoName)
                .dataset (this.dataset)
                .build ();
        modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }
}
