package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.train.TracedProcessWrapper;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoMongoService;
import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;

@Slf4j
@Component
public class SaveOutput2MemoryProcessor implements ModelPostProcessor{
    @Override
    public void process(TracedProcessWrapper processWrapper) {
        log.info ("SaveOutput2MemoryProcessor 只保存modelInfo和statistics");

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
        String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (processWrapper.getDataset (),
                processWrapper.getAlgoName (), processWrapper.getPhase (), null, modelInfoPrefix);
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
