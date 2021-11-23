package edu.nwpu.cpuis.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatasetLoaderTest {
    @Resource
    private DatasetLoader loader;

    @Test
    void loadDataset() throws IOException {
        loader.loadDataset ("E:/inputs/fb", "fb");
    }
}
