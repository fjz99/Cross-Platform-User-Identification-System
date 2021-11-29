package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.DatasetManageEntity;
import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.ModelInfo;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.nwpu.cpuis.entity.ErrCode.ALGO_NOT_EXISTS;
import static edu.nwpu.cpuis.train.PythonScriptRunner.getDirectoryPath;
import static edu.nwpu.cpuis.train.PythonScriptRunner.modelInfoPrefix;
import static edu.nwpu.cpuis.utils.ModelKeyGenerator.generateKeyWithIncId;

//type=null
@Service
@Slf4j
public class TracedModelService {
    private static final String datasetKey = "dirs";
    public static final String PREDICT_PHASE = "predict";
    @Resource
    private MongoService<ModelInfo> service;
    @Value("${file.input-base-location}")
    private String datasetPath;
    @Resource
    private DatasetService datasetService;
    @Resource
    private AlgoService algoService;
    @Resource
    private MongoService<Map> mapMongoService;

    public static String getDatasetKey() {
        return datasetKey;
    }

    public List<ModelVO> query(ModelSearchVO searchVO) {
        if (searchVO.getPageSize () == null) {
            searchVO.setPageSize (20);
        }
        if (searchVO.getPageNum () == null) {
            searchVO.setPageNum (1);
        }
        String key = ModelKeyGenerator.generateModelInfoKey (searchVO.getDataset (), searchVO.getAlgoName (),
                searchVO.getPhase (), null, modelInfoPrefix);
        List<ModelInfo> modelInfos = service.selectList (key, ModelInfo.class, searchVO.getPageNum (), searchVO.getPageSize ());
        List<ModelVO> VOS = new ArrayList<> ();
        for (ModelInfo modelInfo : modelInfos) {
            AlgoEntity algoInfo = algoService.getAlgoEntity (modelInfo.getAlgo ());
            DatasetManageEntity[] manageEntities = new DatasetManageEntity[2];
            manageEntities[0] = datasetService.getEntity (modelInfo.getDataset ()[0]);
            manageEntities[1] = datasetService.getEntity (modelInfo.getDataset ()[1]);
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
                    .datasetInfo (Arrays.asList (manageEntities))
                    .statistics (statistics)
                    .build ();
            VOS.add (vo);
        }
        return VOS;
    }

    /**
     * 调用是肯定没有在训练中的
     *
     * @param datasets 文件夹，2个
     * @return 模型id，如果错误，返回-1
     */
    public int train(List<String> datasets, String name, Map<String, String> args) {
        checkAlgoAndDataset (name, datasets);
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
        return PythonScriptRunner.runTracedScript (name, algoService.getAlgoEntity (name).getTrainSource (), map, datasets, "train");
    }

    private void checkAlgoAndDataset(String algo, List<String> datasets) {
        if (!algoService.exists (algo)) {
            throw new CpuisException (ALGO_NOT_EXISTS, algo);
        }
        Assert.isTrue (datasets.size () == 2, "");
        for (String dataset : datasets) {
            if (!datasetService.exists (dataset)) {
                throw new CpuisException (ErrCode.DATASET_NOT_EXISTS);
            }
        }
    }

    public void predict(PredictVO vo) {
        String[] dataset = vo.getDataset ().toArray (new String[]{});
        int id = vo.getId ();
        String name = vo.getAlgoName ();
        List<String> datasets = vo.getDataset ();

        if (id == -1) {
            id = getLatestId (name, dataset, PREDICT_PHASE);
        }
        if (!contains (name, dataset, PREDICT_PHASE, id)) {
            throw new CpuisException (ErrCode.MODEL_NOT_EXISTS);
        }

        checkAlgoAndDataset (name, datasets);
        Map<String, Object> map = new HashMap<> ();
        List<String> out = new ArrayList<> (datasets.size ());
        for (String s : datasets) {
            out.add (datasetService.getDatasetLocation (s));
        }
        map.put (datasetKey, out);

        String directoryPath = getDirectoryPath (name, dataset, PREDICT_PHASE, id);
        map.put ("inputDir", directoryPath);//!!
        //fixme
        PythonScriptRunner.runTracedScript (name, algoService.getAlgoEntity (name).getPredictSource (), map, datasets, PREDICT_PHASE);
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
    public boolean delete(ModelLocationVO vo) {
        String key = ModelKeyGenerator.generateModelInfoKey (vo.getDataset (), vo.getAlgoName (),
                vo.getPhase (), null, modelInfoPrefix);
        ModelInfo modelInfo = service.selectById (vo.getId (), ModelInfo.class, key);
        if (modelInfo != null) {
            service.deleteByEqualGeneric (vo.getId (), ModelInfo.class, key, "id");
            service.deleteCollection (modelInfo.getOutputCollectionName ());
            service.deleteCollection (modelInfo.getReversedOutputCollectionName ());
            service.deleteCollection (modelInfo.getStatisticsCollectionName ());
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

    public boolean contains(String algoName, String[] dataset, String phase, Integer id) {
        String key = ModelKeyGenerator.generateModelInfoKey (dataset, algoName, phase, null, modelInfoPrefix);
        ModelInfo modelInfo = service.selectById (id, ModelInfo.class, key);
        return modelInfo != null;
    }
}
