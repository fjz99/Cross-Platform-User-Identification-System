package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoOutputEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MongoServiceTest {
    String name = "hash-fb-fs-train-output";
    String name2 = "hash-fs-fb-train-output";
    @Autowired
    private MongoService<MongoOutputEntity> service;

    @Test
    public void prepareData() {
        if (service.collectionExists (name)) {
            service.deleteCollection (name);
        }
        service.createCollection (name);
        MongoOutputEntity info = new MongoOutputEntity ();
        info.setId (1);
        info.setOthers (new ArrayList<MongoOutputEntity.OtherUser> () {
            {
                add (MongoOutputEntity.OtherUser.builder ().id (2).similarity (0.5).build ());
                add (MongoOutputEntity.OtherUser.builder ().id (3).similarity (0.6).build ());
            }
        });
        service.insert (info, name);
        info.setId (6);
        info.setOthers (new ArrayList<MongoOutputEntity.OtherUser> () {
            {
                add (MongoOutputEntity.OtherUser.builder ().id (7).similarity (0.5).build ());
                add (MongoOutputEntity.OtherUser.builder ().id (8).similarity (0.6).build ());
            }
        });
        service.insert (info, name);
//        service.createIndex (name, "id", true, true); //id自带索引了
    }

    @Test
    public void search() {
        System.out.println (service.selectById (1, MongoOutputEntity.class, name));
        System.out.println (service.selectList (name, MongoOutputEntity.class, 0, 10));
        System.out.println (service.selectRange (name, MongoOutputEntity.class, 0, 20, 3, 22));
    }
}
