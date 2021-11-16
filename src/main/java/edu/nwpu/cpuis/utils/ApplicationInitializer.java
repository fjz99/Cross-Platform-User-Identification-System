package edu.nwpu.cpuis.utils;

import edu.nwpu.cpuis.service.AlgoService;
import edu.nwpu.cpuis.service.DatasetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author fujiazheng
 */
@Component
@Slf4j
public class ApplicationInitializer {
    private final DatasetLoader loader;
    private final DatasetService datasetService;
    private final AlgoService algoService;

    public ApplicationInitializer(DatasetLoader loader, DatasetService datasetService, AlgoService algoService) {
        this.loader = loader;
        this.datasetService = datasetService;
        this.algoService = algoService;
    }

    public void init() {
        datasetService.scanDataset ();
//        datasetService.getDatasetLocation ().forEach ((k, v) -> {
//            loader.loadDataset (v, k);
//        });
        //不需要了，每次上传数据集之后，都会自动扫描至mongo
        algoService.scanAlgo ();
        log.info ("应用程序启动完成");
    }

//    //todo
//    @Scheduled(cron = "0 0 ? * * *")
//    public void ScheduledDatasetLoader() {
//        datasetService.getDatasetLocation ().forEach ((k, v) -> {
//            loader.loadDataset (v, k);
//        });
//    }
}
