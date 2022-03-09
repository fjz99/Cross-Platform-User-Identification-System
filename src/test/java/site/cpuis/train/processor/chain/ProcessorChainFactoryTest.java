package site.cpuis.train.processor.chain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.cpuis.train.output.Stage2Output;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProcessorChainFactoryTest {
    @Autowired
    private ProcessorChainFactory factory;

    @Test
    void getProcessor() {
        ProcessorChain chain = factory.getProcessor (Stage2Output.class);
        System.out.println (chain);
    }
}
