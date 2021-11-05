package edu.nwpu.cpuis.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AlgoServiceTest {
    @Value("${file.algo-base-location}")
    private String algoBaseLocation;

    @Test
    void scanAlgo() {
        File file = new File (algoBaseLocation);
        for (File listFile : file.listFiles ()) {
            System.out.println (listFile.getAbsolutePath ());
        }
    }
}
