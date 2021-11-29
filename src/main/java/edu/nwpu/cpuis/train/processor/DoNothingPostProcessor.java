package edu.nwpu.cpuis.train.processor;

import edu.nwpu.cpuis.entity.Output;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(ProcessorNames.doNothingPostProcessor)
@Slf4j
public class DoNothingPostProcessor extends ModelPostProcessor{
    @Override
    protected void process(Output output, String algoName, List<String> dataset, String phase, int thisId, String directoryPath) {
        log.info ("DoNothingPostProcessor do nothing");
    }
}
