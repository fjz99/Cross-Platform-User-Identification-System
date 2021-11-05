package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.AlgoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AlgoService {
    @Value("${file.algo-base-location}")
    private String algoBaseLocation;
    @Resource
    private MongoService<AlgoEntity> mongoService;
    @Value("${algo-mongo-collection-name}")
    private String algoMongoLocation;

    private Map<String, AlgoEntity> algoMap = new HashMap<> ();

    public boolean exists(String name) {
        return algoMap.containsKey (name);
    }

    public AlgoEntity getAlgoEntity(String name) {
        return algoMap.getOrDefault (name, null);
    }

    public boolean validateSource(String name, String sourceName) {
        return false;
    }

    public void saveToMongoDB(AlgoEntity entity) {
        if (!mongoService.collectionExists (algoMongoLocation)) {
            mongoService.createCollection (algoMongoLocation);
            mongoService.createIndex (algoMongoLocation, "name", true, true);
        }
        mongoService.deleteByEqual (entity.getName (), AlgoEntity.class, algoMongoLocation, "name");
        mongoService.insert (entity, algoMongoLocation);
        algoMap.put (entity.getName (), entity);
    }

    public void scanAlgo() {
        mongoService.selectAll (algoMongoLocation, AlgoEntity.class).forEach (x -> {
            if (algoMap.containsKey (x.getName ())) {
                log.error ("mongoDB中的algo数据重复了");
                return;
            }
            algoMap.put (x.getName (), x);
        });
        log.info ("Auto scan algoEntity from collection {}", algoMongoLocation);
    }
}
