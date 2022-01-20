package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.*;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import edu.nwpu.cpuis.entity.vo.ModelLocationVO;
import edu.nwpu.cpuis.entity.vo.ModelSearchVO;
import edu.nwpu.cpuis.entity.vo.ModelVO;
import edu.nwpu.cpuis.entity.vo.PredictVO;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import edu.nwpu.cpuis.train.State;
import edu.nwpu.cpuis.train.TracedProcessWrapper;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;
import static edu.nwpu.cpuis.utils.ModelKeyGenerator.generateKeyWithIncId;

//type=null
@Service
@Slf4j
public class TracedModelService {
    public static final String PREDICT_PHASE = "predict";
    public static final String TRAIN_PHASE = "train";
    private static final String datasetKey = "dirs";
    private static final String PREDICT_OUTPUT_CACHE_NAME = "predictOutput";
    private static final String tracedModelQueryCacheName = "tracedModelQuery";
    @Resource
    private MongoService<ModelInfo> service;
    @Value("${file.input-base-location}")
    private String datasetPath;
    @Resource
    private DatasetService datasetService;
    @Resource
    private AlgoService algoService;
    @Resource
    @SuppressWarnings("rawtypes")
    private MongoService<Map> mapMongoService;

    public static String getDatasetKey() {
        return datasetKey;
    }

    @Cacheable(cacheNames = tracedModelQueryCacheName,
            key = "T(String).format('%s-%s-%s-%s-%s',#searchVO.algoName,#a0.dataset,#p0.phase,#p0.pageNum,#p0.pageSize)")
    public PageEntity<ModelVO> query(ModelSearchVO searchVO) {
        if (searchVO.getPageSize () == null) {
            searchVO.setPageSize (20);
        }
        if (searchVO.getPageNum () == null) {
            searchVO.setPageNum (1);
        }
        String key = ModelKeyGenerator.generateModelInfoKey (searchVO.getDataset (), searchVO.getAlgoName (),
                searchVO.getPhase (), null, modelInfoPrefix);
        List<ModelInfo> modelInfos = service.selectList (key, ModelInfo.class, searchVO.getPageNum (), searchVO.getPageSize ());
        int allDataNum = (int) service.countAll (key, ModelInfo.class);

        List<ModelVO> VOS = new ArrayList<> ();
        for (ModelInfo modelInfo : modelInfos) {
            AlgoEntity algoInfo = algoService.getAlgoEntity (modelInfo.getAlgo ());
            List<DatasetManageEntity> manageEntities = new ArrayList<> ();
            for (String s : modelInfo.getDataset ()) {
                manageEntities.add (datasetService.getEntity (s));
            }
            Map<?, ?> statistics;
            String key0 = ModelKeyGenerator.generateKey0 (searchVO.getDataset (), searchVO.getAlgoName (), searchVO.getPhase ()
                    , "statistics");
            //relatedId应该为String，类型不同无法匹配。。
            List<Map> r = mapMongoService.selectByEqualsGeneric (key0, Map.class, "relatedId", modelInfo.getId ());
            if (r.size () == 0) {
                statistics = null;
            } else {
                statistics = r.get (0);
            }
            ModelVO vo = ModelVO.builder ()
                    .id (modelInfo.getId ())
                    .time (modelInfo.getTime ())
                    .algoInfo (algoInfo)
                    .datasetInfo (manageEntities)
                    .statistics (statistics)
                    .build ();
            VOS.add (vo);
        }
        return PageEntity.byAllDataNum (VOS, allDataNum, searchVO.getPageNum (), searchVO.getPageSize ());
    }

    /**
     * 调用是肯定没有在训练中的
     *
     * @param datasets 文件夹，2个
     * @return 模型id，如果错误，返回-1
     */
    public int train(List<String> datasets, String name, Map<String, String> args) {
        Map<String, Object> map = new HashMap<> ();
        List<String> out = new ArrayList<> (datasets.size ());
        for (String s : datasets) {
            out.add (datasetService.getDatasetLocation (s));
        }
        map.put (datasetKey, out);
//            map.put ("inputDir", null);//train是没有这个的
//            map.putAll (args);//会覆盖
        //fixme
//            PythonUtils.runScript (name, singleModel.getTrainSource (), map, datasets);
        return PythonScriptRunner.runTracedScript (name, algoService.getAlgoEntity (name).getTrainSource (), map, datasets, TRAIN_PHASE, false).getId ();
    }

    //FIXME 使用文件无法使用缓存
    @Cacheable(cacheNames = PREDICT_OUTPUT_CACHE_NAME,
            key = "T(String).format('%s-%s-%s-%s',#a0.algoName,#a0.dataset,#a0.input,#inputs)",
            condition = "not #inputs.containsKey('file')")
    public @NonNull
    PythonScriptRunner.TracedScriptOutput predict(PredictVO vo, Map<String, String> inputs) {
        String[] dataset = vo.getDataset ().toArray (new String[]{});
        String name = vo.getAlgoName ();
        if (!contains (name, dataset, TRAIN_PHASE, 0)) {
            throw new CpuisException (ErrCode.MODEL_NOT_EXISTS);
        }
        Map<String, Object> map = new HashMap<> ();
//        List<String> out = new ArrayList<> (datasets.size ());
//        for (String s : datasets) {
//            out.add (datasetService.getDatasetLocation (s));
//        }
//        map.put (datasetKey, out);

//        String directoryPath = getDirectoryPath (name, dataset, TRAIN_PHASE, 0);
//        map.put ("inputDir", directoryPath);//!!
//        map.put ("input", vo.getInput ());//!!
//        map.put ("input", input);
        map.putAll (inputs);
        //fixme
        PythonScriptRunner.TracedScriptOutput output = PythonScriptRunner.runTracedScript (name,
                algoService.getAlgoEntity (name).getPredictSource (), map, new ArrayList<> (), PREDICT_PHASE, true);
        return output;
    }

    public boolean destroy(String name) {
        if (PythonScriptRunner.getTracedProcess (name) == null) {
            log.info ("模型删除失败");
            return false;
        } else {
            PythonScriptRunner.getTracedProcess (name).removeFromMap ();
            log.info ("模型删除成功");
            return true;
        }
    }

    public Double getPercentage(String name) {
        TracedProcessWrapper trainProcess = PythonScriptRunner.getTracedProcess (name);
        if (trainProcess == null) {
            return null;
        }
        return trainProcess.getPercentage ();
    }

    public Double getPercentage(ModelLocationVO vo) {
        String name = generateKeyWithIncId (vo.getDataset (), vo.getAlgoName (), vo.getPhase (), null, vo.getId ());
        TracedProcessWrapper trainProcess = PythonScriptRunner.getTracedProcess (name);
        if (trainProcess == null) {
            return null;
        }
        return trainProcess.getPercentage ();
    }

    //用于判断是否正在训练中
    public boolean isTraining(String name, String[] dataset, String phase, boolean replaceStopped, int id) {
        String k;
        if (id == -1) {
            k = getLatestKey (name, dataset, phase);
        } else {
            k = generateKeyWithIncId (dataset, name, phase, null, id);
        }
        if (!replaceStopped)
            return PythonScriptRunner.getTracedProcess (k) != null;
        else
            return PythonScriptRunner.getTracedProcess (k) != null && PythonScriptRunner.getTracedProcess (k).getState () == State.TRAINING;
    }

    public String getLatestKey(String name, String[] dataset, String phase) {
        int id = getLatestId (name, dataset, phase);
        return generateKeyWithIncId (dataset, name, phase, null, id);
    }

    public String getKey(String name, String[] dataset, String phase, String type, Integer id) {
        return generateKeyWithIncId (dataset, name, phase, type, id);
    }

    public int getLatestId(String name, String[] dataset, String phase) {
        String key = ModelKeyGenerator.generateModelInfoKey (dataset, name,
                phase, null, modelInfoPrefix);
        List<ModelInfo> modelInfos = service.selectAll (key, ModelInfo.class);
        return modelInfos
                .stream ()
                .map (ModelInfo::getId)
                .max (Comparator.naturalOrder ())
                .orElse (-1) + 1;
    }

    public String getStatus(String name) {
        return PythonScriptRunner.getTracedProcess (name).getState ().toString ();
    }

    /**
     * @return model exists
     */
    @CacheEvict(cacheNames = {PREDICT_OUTPUT_CACHE_NAME, tracedModelQueryCacheName}, allEntries = true)
    public boolean delete(ModelLocationVO vo) {
        String key = ModelKeyGenerator.generateModelInfoKey (vo.getDataset (), vo.getAlgoName (),
                vo.getPhase (), null, modelInfoPrefix);
        ModelInfo modelInfo = service.selectById (vo.getId (), ModelInfo.class, key);
        if (modelInfo != null) {
            service.deleteByEqualGeneric (vo.getId (), ModelInfo.class, key, "id");
            deleteCollection (modelInfo.getOutputCollectionName ());
            deleteCollection (modelInfo.getReversedOutputCollectionName ());
            deleteCollection (modelInfo.getStatisticsCollectionName ());
            try {
                FileUtils.deleteDirectory (new File (modelInfo.getDataLocation ()));
            } catch (IOException e) {
                e.printStackTrace ();
            }
            log.info ("delete vo {},collection {},{},{} ,files {}", vo, modelInfo.getOutputCollectionName (),
                    modelInfo.getReversedOutputCollectionName (), modelInfo.getStatisticsCollectionName (), modelInfo.getDataLocation ());
            return true;
        } else {
            return false;
        }
    }

    private void deleteCollection(String collection) {
        if (!StringUtils.isEmpty (collection)) {
            service.deleteCollection (collection);
        }
    }

    public boolean contains(String algoName, String[] dataset, String phase, Integer id) {
        String key = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
        ModelInfo modelInfo = service.selectById (id, ModelInfo.class, key);
        return modelInfo != null;
    }
}
