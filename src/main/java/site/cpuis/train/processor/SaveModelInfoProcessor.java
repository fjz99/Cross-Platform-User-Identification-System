package site.cpuis.train.processor;

import site.cpuis.entity.ModelInfo;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import site.cpuis.train.PythonScriptRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component(ProcessorNames.doNothingPostProcessor)
@Slf4j
@Order
public class SaveModelInfoProcessor implements ModelPostProcessor {
    @Override
    public void process(TracedProcessWrapper processWrapper) {
        log.info ("DoNothingPostProcessor DoNothing");
        String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (processWrapper.getDataset (),
                processWrapper.getAlgoName (), processWrapper.getPhase (), null, PythonScriptRunner.modelInfoPrefix);
        PythonScriptRunner.modelInfoMongoService.deleteCollection (modelInfoKey);
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

        if (!PythonScriptRunner.modelInfoMongoService.collectionExists (modelInfoKey)) {
            PythonScriptRunner.modelInfoMongoService.createCollection (modelInfoKey);
        }
        PythonScriptRunner.modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    @Override
    public boolean supports(Class<?> outputType) {
        return true;
    }
}
