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
}
