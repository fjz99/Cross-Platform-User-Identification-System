package edu.nwpu.cpuis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ScheduledTaskService {
    private static final Logger log = LoggerFactory.getLogger (ScheduledTaskService.class);
    private final DatasetService service;

    public ScheduledTaskService(DatasetService service) {
        this.service = service;
    }

    /**
     * 检查数据集一致性，即map和mongo和文件夹一致性，如果不一致，就会删除数据集
     */
    //todo
    public void checkDatasetConsistency() {
        service.getDatasetLocation ().forEach ((k, v) -> {

        });
    }
}
