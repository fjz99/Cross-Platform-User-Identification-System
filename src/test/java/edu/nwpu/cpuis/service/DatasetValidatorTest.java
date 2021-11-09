package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.service.validator.DatasetValidator;
import org.junit.jupiter.api.Test;

class DatasetValidatorTest {

    @Test
    void validateFileType() {
        System.out.println (new DatasetValidator ().validateFileType ("E:\\inputs\\test", "txt"));
    }
}
