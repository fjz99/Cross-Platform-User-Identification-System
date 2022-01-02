package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.train.TracedProcessWrapper;
import edu.nwpu.cpuis.train.output.Stage2Output;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Stage2PostProcessorTest {
    @Resource
    Stage2PostProcessor processor;

    @Test
    void process() {
        TracedProcessWrapper wrapper = mock (TracedProcessWrapper.class);
        List<String> c = new ArrayList<> ();
        c.add ("xxx");
        c.add ("yyy");
        c.add ("zzz");
        Object o = Stage2Output
                .builder ()
                .name ("aa")
                .content (c)
                .originalLabel (c)
                .predictedLabel (c);
        when (wrapper.getOutputData ()).thenReturn (o);

        processor.process (wrapper);
    }
}
