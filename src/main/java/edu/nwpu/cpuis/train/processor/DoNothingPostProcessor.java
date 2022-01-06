package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.train.TracedProcessWrapper;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoMongoService;
import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;

@Component(ProcessorNames.doNothingPostProcessor)
@Slf4j
@Order
public class DoNothingPostProcessor implements ModelPostProcessor {
    @Override
    public void process(TracedProcessWrapper processWrapper) {
        log.info ("DoNothingPostProcessor DoNothing");
        String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (processWrapper.getDataset (),
                processWrapper.getAlgoName (), processWrapper.getPhase (), null, modelInfoPrefix);
        modelInfoMongoService.deleteCollection (modelInfoKey);
        //下面保存ModelInfo等
        ModelInfo modelInfo = ModelInfo.builder ()
                .id (processWrapper.getThisId ())
                .time (LocalDateTime.now ())
                .dataLocation (processWrapper.getDirectoryPath ())
                .statisticsCollectionName ("")
                .outputCollectionName ("")
                .reversedOutputCollectionName ("")
                .algo (processWrapper.getAlgoName ())
                .dataset (processWrapper.getDataset ())
                .build ();

        if (!modelInfoMongoService.collectionExists (modelInfoKey)) {
            modelInfoMongoService.createCollection (modelInfoKey);
        }
        modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    @Override
    public boolean supports(Class<?> outputType) {
        return true;
    }
}
