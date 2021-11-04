package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoOutputEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

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
        service.createTextIndex (name, "userName", false);
        service.createIndex (name, "userName", false, true);
        MongoOutputEntity info = new MongoOutputEntity ();
        info.setUserName ("fjzfjzfjzfjzfjzfjzfjz");
        info.setOthers (new ArrayList<MongoOutputEntity.OtherUser> () {
            {
                add (MongoOutputEntity.OtherUser.builder ().userName ("fjz").similarity (0.5).build ());
                add (MongoOutputEntity.OtherUser.builder ().userName ("zjf").similarity (0.6).build ());
            }
        });
        service.insert (info, name);
        info.setUserName ("zryzryzryzryzryzryzryzryzry");
        info.setOthers (new ArrayList<MongoOutputEntity.OtherUser> () {
            {
                add (MongoOutputEntity.OtherUser.builder ().userName ("zry").similarity (0.5).build ());
                add (MongoOutputEntity.OtherUser.builder ().userName ("fjz").similarity (0.6).build ());
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

    @Test
    public void search2() {
        System.out.println (service.searchRegex (MongoOutputEntity.class, name, "\\w+", 1, 20));
        System.out.println (service.searchFullText (MongoOutputEntity.class, name, "zrz", 1, 20));
    }

    @Test
    public void search3() {
        System.out.println (service.searchRegex (MongoOutputEntity.class, name, "\\w+", 1, 1));
    }

}
