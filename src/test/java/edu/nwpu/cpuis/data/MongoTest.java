package edu.nwpu.cpuis.data;

import edu.nwpu.cpuis.entity.MongoEntity;
import edu.nwpu.cpuis.service.MongoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.Map;


//@DataMongoTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MongoTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoService<MongoEntity> mongoService;

    @Test
    public void test() {
        Map<String, Object> map = new HashMap<> ();
        map.put ("output", new HashMap<Integer, double[][]> () {
            {
                put (1, new double[][]{
                        {1, 0.6}, {2, 0.3}
                });
            }
        });
        String key = "demo-d1-d2-train";
        map.put ("_id", key);
        mongoTemplate.insert (map, "output");
    }

    @Test
    public void test2() {
        MongoEntity output = mongoService.selectById ("demo-abc.txt-def.txt-train", MongoEntity.class, "output");
        System.out.println (output);
    }
}
