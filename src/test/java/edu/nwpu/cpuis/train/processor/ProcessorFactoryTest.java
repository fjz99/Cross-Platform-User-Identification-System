package edu.nwpu.cpuis.train.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProcessorFactoryTest {
    @Autowired
    private ProcessorFactory factory;

    @Test
    void getProcessor() {
        System.out.println (factory.getMap ());
    }
}
