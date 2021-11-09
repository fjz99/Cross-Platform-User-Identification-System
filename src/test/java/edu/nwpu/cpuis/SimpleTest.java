package edu.nwpu.cpuis;

import edu.nwpu.cpuis.service.DatasetService;
import org.junit.jupiter.api.Test;

/**
 * @date 2021/11/9 18:15
 */
public class SimpleTest {
    @Test
    public void test(){
        System.out.println (new DatasetService ().getDatasetSizePretty (10000));
    }
}
