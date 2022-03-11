package site.cpuis.train.processor;

import site.cpuis.entity.ModelInfo;
import site.cpuis.entity.MongoOutputEntity;
import site.cpuis.entity.Output;
import site.cpuis.train.PythonScriptRunner;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.train.output.MatrixSimilarityOutput;
import site.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 处理矩阵形式的输出
 * 矩阵为m x m的每个元素是一个二元组(userName,value)
 * <p>
 * 这个是为了兼容算法1,3
 */
@Slf4j
@Component(ProcessorNames.matrixValuePostProcessor)
public class MatrixValuePostProcessor implements ModelPostProcessor {
    protected Output output;
    protected int thisId;
    protected String[] dataset;
    protected String algoName;
    protected String phase;
    protected String directoryPath;//模型的checkpoint文件存储位置位置
    protected String modelInfoKey;

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
                return ModelKeyGenerator.generateKeyWithIncId (reversedDataset, algoName, phase, type, thisId, false) + "TRACED_WRAPPER";
            else return ModelKeyGenerator.generateKeyWithIncId (reversedDataset, algoName, phase, type, thisId, false);
        else if (type.equals ("statistics"))
            return ModelKeyGenerator.generateKey0 (reversedDataset, algoName, phase, type);
        else
            return ModelKeyGenerator.generateKey (reversedDataset, algoName, phase, type, false);
    }

    private Runnable reversedOutput(final boolean saveStatistics2Mongo) {
        return () -> {
            String key = getReversedKey (PythonScriptRunner.OUTPUT_TYPE, thisId);
            //检查mongoCollection
            if (PythonScriptRunner.mongoService.collectionExists (key)) {
                PythonScriptRunner.mongoService.deleteCollection (key);
                log.warn ("{} 输出的mongo collection已存在", key);
            }
            PythonScriptRunner.mongoService.createCollection (key);
            PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);
            PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);

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
                saveStatisticsToMongoDB (getReversedKey ("statistics", null), output);
            }
            log.info ("reversed output {} saved", key);
        };
    }

    @Override
    public void process(TracedProcessWrapper processWrapper) {
        this.output = (Output) processWrapper.getOutputData ();
        this.algoName = processWrapper.getAlgoName ();
        this.dataset = processWrapper.getDataset ();
        this.phase = processWrapper.getPhase ();
        this.thisId = processWrapper.getThisId ();
        this.directoryPath = processWrapper.getDirectoryPath ();
        this.modelInfoKey = ModelKeyGenerator.generateModelInfoKey (this.dataset, algoName, phase, null, PythonScriptRunner.modelInfoPrefix);


        //检查mongoCollection
        String key = ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId, true);
        if (PythonScriptRunner.mongoService.collectionExists (key)) {
            PythonScriptRunner.mongoService.deleteCollection (key);
            log.warn ("{} 输出的mongo collection已存在", key);
        }
        PythonScriptRunner.mongoService.createCollection (key);
        PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);
        PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);
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
            PythonScriptRunner.mongoService.insert (mongoOutputEntity, key);
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
                .outputCollectionName (ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId, true))
                .reversedOutputCollectionName (getReversedKey (PythonScriptRunner.OUTPUT_TYPE, thisId))
                .algo (algoName)
                .dataset (this.dataset)
                .build ();
        PythonScriptRunner.modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    @Override
    public boolean supports(Class<?> outputType) {
        return outputType == MatrixSimilarityOutput.class;
    }
}
