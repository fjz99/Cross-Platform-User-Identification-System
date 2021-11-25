package edu.nwpu.cpuis;

import edu.nwpu.cpuis.service.DatasetService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

/**
 * @date 2021/11/9 18:15
 */
public class SimpleTest {
    @Test
    public void test() {
        System.out.println (new DatasetService ().getDatasetSizePretty (10000));
    }

    @Test
    public void test1() {
        try {
            throw new RuntimeException ("kk");
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println (e.getMessage ());
            Arrays.stream (e.getStackTrace ()).forEach (System.out::println);
        }
    }

    @Test
    public void test2() {
        String s = Optional.ofNullable (null).map (x -> x.toString ()).orElse (null);
        System.out.println (s);
    }
}
