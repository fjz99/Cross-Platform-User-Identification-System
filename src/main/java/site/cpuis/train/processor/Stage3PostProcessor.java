package site.cpuis.train.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.cpuis.entity.ModelInfo;
import site.cpuis.entity.MongoOutputEntity;
import site.cpuis.entity.Output;
import site.cpuis.train.PythonScriptRunner;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.train.output.Stage3Output;
import site.cpuis.utils.ModelKeyGenerator;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class Stage3PostProcessor implements ModelPostProcessor {
    protected Output output;
    protected int thisId;
    protected String[] dataset;
    protected String algoName;
    protected String phase;
    protected String directoryPath;//模型的checkpoint文件存储位置位置
    protected String modelInfoKey;

    private String getReversedKey(Integer thisId) {
        String[] reversedDataset = new String[2];
        if (dataset.length == 0) {
            reversedDataset = dataset;
        } else {
            reversedDataset[0] = dataset[1];
            reversedDataset[1] = dataset[0];
        }
        if (thisId != null)
            return ModelKeyGenerator.generateKeyWithIncId (reversedDataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId);
        else
            return ModelKeyGenerator.generateKey (reversedDataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE);
    }

    private Runnable reversedOutput() {
        return () -> {
            String key = getReversedKey (thisId);
            //检查mongoCollection
            if (PythonScriptRunner.mongoService.collectionExists (key)) {
                PythonScriptRunner.mongoService.deleteCollection (key);
                log.warn ("{} 输出的mongo collection已存在", key);
            }
            PythonScriptRunner.mongoService.createCollection (key);
            PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);
            PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);

            Stage3Output output = (Stage3Output) this.output.getOutput ();
            Map<String, List<Stage3Output.Element>> map = output.getOutput ();
            Map<String, MongoOutputEntity> result = new HashMap<> ();
            for (Map.Entry<String, List<Stage3Output.Element>> stringObjectEntry : map.entrySet ()) {
                final String k = stringObjectEntry.getKey ();
                final List<Stage3Output.Element> v = stringObjectEntry.getValue ();
                for (Stage3Output.Element element : v) {
                    String anotherKey = element.getUserName ();
                    if (!result.containsKey (anotherKey)) {
                        MongoOutputEntity mongoOutputEntity = new MongoOutputEntity ();
                        mongoOutputEntity.setUserName (anotherKey);
                        mongoOutputEntity.setOthers (new ArrayList<> ());
                        result.put (anotherKey, mongoOutputEntity);
                    }
                    MongoOutputEntity.OtherUser build = MongoOutputEntity.OtherUser.builder ()
                            .userName (k).similarity (element.getSimilarity ()).build ();
                    result.get (anotherKey).getOthers ().add (build);
                }
            }

            result.forEach ((k, v) -> {
                v.getOthers ().sort (Comparator.comparing (MongoOutputEntity.OtherUser::getSimilarity).reversed ());
                PythonScriptRunner.mongoService.insert (v, key);
            });


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
        String key = ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId);
        if (PythonScriptRunner.mongoService.collectionExists (key)) {
            PythonScriptRunner.mongoService.deleteCollection (key);
            log.warn ("{} 输出的mongo collection已存在", key);
        }
        PythonScriptRunner.mongoService.createCollection (key);
        PythonScriptRunner.mongoService.createTextIndex (key, "userName", false);
        PythonScriptRunner.mongoService.createIndex (key, "userName", false, true);
        Stage3Output output = (Stage3Output) this.output.getOutput ();

        for (Map.Entry<String, List<Stage3Output.Element>> stringObjectEntry : output.getOutput ().entrySet ()) {
            final String k = stringObjectEntry.getKey ();
            final List<Stage3Output.Element> v =  stringObjectEntry.getValue ();

            MongoOutputEntity mongoOutputEntity = new MongoOutputEntity ();
            mongoOutputEntity.setUserName (k);
            mongoOutputEntity.setOthers (new ArrayList<> ());
            for (Stage3Output.Element x : v) {
                MongoOutputEntity.OtherUser otherUser = MongoOutputEntity.OtherUser.builder ()
                        .userName (x.getUserName ())
                        .similarity (x.getSimilarity ())
                        .build ();
                mongoOutputEntity.getOthers ().add (otherUser);
            }
            mongoOutputEntity.getOthers ().sort (Comparator.comparing (MongoOutputEntity.OtherUser::getSimilarity).reversed ());
            PythonScriptRunner.mongoService.insert (mongoOutputEntity, key);
//            log.debug ("{} data is",mongoOutputEntity);
        }


        log.info ("{} saved output to mongodb", key);
        PythonScriptRunner.executor.submit (reversedOutput ());

        //下面保存ModelInfo等
        ModelInfo modelInfo = ModelInfo.builder ()
                .id (thisId)
                .time (LocalDateTime.now ())
                .dataLocation (directoryPath)
//                .statisticsCollectionName (key)
                .outputCollectionName (ModelKeyGenerator.generateKeyWithIncId (this.dataset, algoName, phase, PythonScriptRunner.OUTPUT_TYPE, thisId))
                .reversedOutputCollectionName (getReversedKey (thisId))
                .algo (algoName)
                .dataset (this.dataset)
                .build ();
        PythonScriptRunner.modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    @Override
    public boolean supports(Class<?> outputType) {
        return outputType == Stage3Output.class;
    }
}
