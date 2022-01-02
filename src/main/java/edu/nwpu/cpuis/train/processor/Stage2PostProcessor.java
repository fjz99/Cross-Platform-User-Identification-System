package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import edu.nwpu.cpuis.train.TracedProcessWrapper;
import edu.nwpu.cpuis.train.output.Stage2Output;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoMongoService;
import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;

/**
 * 不需要reverse
 */
@Component
@Order(1)
@Slf4j
public class Stage2PostProcessor implements ModelPostProcessor {

    @Resource
    private MongoService<Stage2Output> mongoService;

    @Override
    public void process(TracedProcessWrapper wrapper) {
        String key = ModelKeyGenerator.generateKeyWithIncId (wrapper.getDataset (), wrapper.getAlgoName (),
                wrapper.getPhase (), PythonScriptRunner.OUTPUT_TYPE, wrapper.getThisId ());
        if (mongoService.collectionExists (key)) {
            mongoService.deleteCollection (key);
            log.warn ("{} 输出的mongo collection已存在,覆盖", key);
        }
        mongoService.createCollection (key);
        mongoService.createTextIndex (key, "name", false);
        mongoService.createIndex (key, "name", false, true);
        @SuppressWarnings("unchecked")
        List<Stage2Output> output = (List<Stage2Output>) wrapper.getOutputData ();
        mongoService.insertMulti (output, key);
        log.info ("{} output 已插入到mongoDB中", key);

        //下面保存ModelInfo等
        ModelInfo modelInfo = ModelInfo.builder ()
                .id (wrapper.getThisId ())
                .time (LocalDateTime.now ())
                .dataLocation (wrapper.getDirectoryPath ())
                .statisticsCollectionName ("")
                .outputCollectionName (key)
                .reversedOutputCollectionName ("")
                .algo (wrapper.getAlgoName ())
                .dataset (wrapper.getDataset ())
                .build ();
        String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (wrapper.getDataset (),
                wrapper.getAlgoName (), wrapper.getPhase (), null, modelInfoPrefix);
        if (!modelInfoMongoService.collectionExists (modelInfoKey)) {
            modelInfoMongoService.createCollection (modelInfoKey);
        }
        modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    //FIXME
    @Override
    public boolean supports(Class<?> outputType) {
        return outputType == List.class;
    }
}
