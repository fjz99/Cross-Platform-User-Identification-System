package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.PageEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlgoService {
    private final Map<String, AlgoEntity> algoMap = new HashMap<> ();
    @Value("${file.algo-base-location}")
    private String algoBaseLocation;
    @Resource
    private MongoService<AlgoEntity> mongoService;
    @Value("${algo-mongo-collection-name}")
    private String algoMongoLocation;

    public boolean exists(String name) {
        return algoMap.containsKey (name);
    }

    public String getAlgoLocation(String name) {
        return algoBaseLocation + "/" + name + "/";
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

    @CacheEvict(cacheNames = "query", allEntries = true)
    public void delete(String name) throws IOException {
        if (!algoMap.containsKey (name)) {
            return;
        }
        AlgoEntity algoEntity = algoMap.get (name);
        algoMap.remove (name);
        mongoService.deleteByEqual (algoEntity.getName (), AlgoEntity.class, algoMongoLocation, "name");
        //删除文件
        String loc = getAlgoLocation (name);
        FileUtils.deleteDirectory (new File (loc));
        log.info ("Delete algo {} from collection {},{} ok.", algoEntity.getName (), algoMongoLocation, loc);
    }

    @Cacheable(key = "T(String).format('query-%s-%s',#size,#num)", cacheNames = "query")
    public PageEntity<AlgoEntity> query(Integer size, Integer num) {
        List<AlgoEntity> algoEntities = mongoService.selectList (algoMongoLocation, AlgoEntity.class, num, size);
        int n = (int) mongoService.countAll (algoMongoLocation, AlgoEntity.class);
        return PageEntity.byAllDataNum (algoEntities, n, num, size);
    }
}
