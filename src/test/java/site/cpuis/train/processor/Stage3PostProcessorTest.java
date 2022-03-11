package site.cpuis.train.processor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.cpuis.train.TracedProcessWrapper;
import site.cpuis.train.output.Stage3Output;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage3PostProcessorTest {

    @Autowired
    private Stage3PostProcessor postProcessor;

    @Test
    void process() throws IOException {
        String path = getClass ().getClassLoader ().getResource ("out.json").getPath ();
        String s = String.join ("", FileUtils.readLines (new File (path)));
        Stage3Output output = JSON.parseObject (s, Stage3Output.class);

        TracedProcessWrapper mock = mock (TracedProcessWrapper.class);

        when (mock.getThisId ()).thenReturn (0);
        when (mock.getDataset ()).thenReturn (new String[]{"fb", "fs"});
        when (mock.getPhase ()).thenReturn ("train");
        when (mock.getAlgoName ()).thenReturn ("test");
        when (mock.getOutputData ()).thenReturn (output);


        postProcessor.process (mock);
    }
}
