package edu.nwpu.cpuis.utils;

import edu.nwpu.cpuis.service.DatasetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationInitializer {
    private final DatasetLoader loader;
    private final DatasetService datasetService;

    public ApplicationInitializer(DatasetLoader loader, DatasetService datasetService) {
        this.loader = loader;
        this.datasetService = datasetService;
    }

    public void init() {
        datasetService.scanDataset ();
        datasetService.getDatasetLocation ().forEach ((k, v) -> {
            loader.loadDataset (v, k);
        });
        log.info ("应用程序启动完成");
    }
}
