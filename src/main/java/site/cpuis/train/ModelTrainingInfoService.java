package site.cpuis.train;

import lombok.extern.slf4j.Slf4j;
import site.cpuis.entity.ModelTrainingInfo;
import site.cpuis.entity.vo.ModelLocationVO;
import site.cpuis.service.DataBaseService;
import site.cpuis.utils.ModelKeyGenerator;
import site.cpuis.websocket.ModelStateServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component("modelTrainingInfoService")
@Slf4j
public class ModelTrainingInfoService {
    public static final String MODEL_INFO_CACHE_NAME = "MODEL_INFO_CACHE_NAME";
    /**
     * 因为自己被proxy了
     */
    @Resource
    private ModelTrainingInfoService thisService;
    @Autowired
    private CacheManager manager;
    @Autowired
    private DataBaseService<ModelTrainingInfo> service;
    @Value("${modelTrainingInfoMongoName}")
    private String modelTrainingInfoMongoName;

    @CachePut(cacheNames = MODEL_INFO_CACHE_NAME, key = "#info.id", condition = "#info != null")
    public ModelTrainingInfo setCache(ModelTrainingInfo info) {
//        manager.getCache (MODEL_INFO_CACHE_NAME).put (key, info);
        try {
            ModelStateServer.changeState (info);
        } catch (IOException e) {
            e.printStackTrace ();
        }
        return info;
    }

    @CacheEvict(cacheNames = MODEL_INFO_CACHE_NAME, key = "#a0")
    public void deleteInfo(String key) {
        service.deleteById (key, ModelTrainingInfo.class, modelTrainingInfoMongoName);
    }

    public void deleteInfo(ModelLocationVO vo) {
        String key = ModelKeyGenerator.generateKey (vo.getDataset (), vo.getAlgoName (), vo.getPhase (), null);
        thisService.deleteInfo (key);//因为自己被proxy了
    }

    @Cacheable(cacheNames = MODEL_INFO_CACHE_NAME, key = "#a0", unless = "#result != null")
    public ModelTrainingInfo getInfo(String key) {
        return service.selectById (key, ModelTrainingInfo.class, modelTrainingInfoMongoName);
    }

    public ModelTrainingInfo getInfo(ModelLocationVO vo) {
        String key = ModelKeyGenerator.generateKey (vo.getDataset (), vo.getAlgoName (), vo.getPhase (), null);
        return thisService.getInfo (key);
    }

    public List<ModelTrainingInfo> getModels(String stage) {
        List<ModelTrainingInfo> list = service.selectAll (modelTrainingInfoMongoName, ModelTrainingInfo.class);
        List<ModelTrainingInfo> res = new ArrayList<> ();
        for (ModelTrainingInfo info : list) {
            if (info.getAlgo ().getStage ().equals (stage)) {
                res.add (info);
            }
        }
        return res;
    }

    public List<ModelTrainingInfo> getModels() {
        return service.selectAll (modelTrainingInfoMongoName, ModelTrainingInfo.class);
    }

    //允许put null
    @CachePut(cacheNames = MODEL_INFO_CACHE_NAME, key = "#a0.id")
    public ModelTrainingInfo setCacheAndMongo(ModelTrainingInfo info) {
        insertModelTrainingInfo (info);
        return info;
    }

    public void insertModelTrainingInfo(ModelTrainingInfo modelTrainingInfo) {
        if (!service.collectionExists (modelTrainingInfoMongoName)) {
            service.createCollection (modelTrainingInfoMongoName);
            log.info ("create {}", modelTrainingInfoMongoName);
        }

        String id = modelTrainingInfo.getId ();
        if (service.countNormal ("id", ModelTrainingInfo.class, modelTrainingInfoMongoName, id) == 0) {
            service.insert (modelTrainingInfo, modelTrainingInfoMongoName);
        } else {
            service.updateById (id, modelTrainingInfoMongoName, modelTrainingInfo);
        }
        log.debug ("update modelTrainingInfo of {}", id);
    }

}
