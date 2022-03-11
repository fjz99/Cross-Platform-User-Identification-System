package site.cpuis.train.output;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

class Stage3OutputTest {
    @Test
    void run() {
        String s = "{\"Jeff\":[\"id\":1,\"name\":\"dds\",\"similarity\":0.2}]}";
        Stage3Output output = JSON.parseObject (s, Stage3Output.class);
        System.out.println (output);
    }
}
