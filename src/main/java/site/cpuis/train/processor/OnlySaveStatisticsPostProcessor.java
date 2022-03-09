package site.cpuis.train.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.cpuis.train.PythonScriptRunner;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.train.output.StatisticsOutput;
import site.cpuis.utils.ModelKeyGenerator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 类似于DoNothing的保底式的处理器，会保存统计信息
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 1)
public class OnlySaveStatisticsPostProcessor implements ModelPostProcessor {
    @Override
    public void process(TracedProcessWrapper processWrapper) {
        log.info ("OnlySaveStatisticsPostProcessor 保存modelInfo和statistics");
        String key0 = ModelKeyGenerator.generateKey0 (processWrapper.getDataset (), processWrapper.getAlgoName (), processWrapper.getPhase ()
                , "statistics");
        Map<String, Object> statistics = new HashMap<> ();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
        String format = dateTimeFormatter.format (LocalDateTime.now (ZoneOffset.ofHours (8)));
        statistics.put ("trainTimeStamp", format);
        StatisticsOutput outputData = ((StatisticsOutput) processWrapper.getOutputData ());
        statistics.put ("time", outputData.getTime ());
        statistics.put ("relatedId", processWrapper.getThisId ());
        statistics.putAll (outputData.getStatistics ());
        PythonScriptRunner.mapMongoService.insert (statistics, key0);

        //下面保存ModelInfo等
//        ModelInfo modelInfo = ModelInfo.builder ()
//                .id (processWrapper.getThisId ())
//                .time (LocalDateTime.now ())
//                .dataLocation (processWrapper.getDirectoryPath ())
//                .statisticsCollectionName (key0)
//                .outputCollectionName ("")
//                .reversedOutputCollectionName ("")
//                .algo (processWrapper.getAlgoName ())
//                .dataset (processWrapper.getDataset ())
//                .build ();
//        String modelInfoKey = ModelKeyGenerator.generateModelInfoKey (processWrapper.getDataset (),
//                processWrapper.getAlgoName (), processWrapper.getPhase (), null, modelInfoPrefix);
//        if (!modelInfoMongoService.collectionExists (modelInfoKey)) {
//            modelInfoMongoService.createCollection (modelInfoKey);
//        }
//        modelInfoMongoService.insert (modelInfo, modelInfoKey);
    }

    @Override
    public boolean supports(Class<?> outputType) {
        return StatisticsOutput.class.isAssignableFrom (outputType);//判断是不是子类
    }
}
