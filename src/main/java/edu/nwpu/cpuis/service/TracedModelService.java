package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.DatasetManageEntity;
import edu.nwpu.cpuis.entity.ModelInfo;
import edu.nwpu.cpuis.entity.vo.ModelLocationVO;
import edu.nwpu.cpuis.entity.vo.ModelSearchVO;
import edu.nwpu.cpuis.entity.vo.ModelVO;
import edu.nwpu.cpuis.train.ProcessWrapper;
import edu.nwpu.cpuis.train.PythonUtils;
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

import static edu.nwpu.cpuis.train.PythonUtils.modelInfoPrefix;

@Service
@Slf4j
public class TracedModelService {
    private static final String datasetKey = "dirs";
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
     * @param datasets 文件夹，2个
     * @return 模型id，如果错误，返回-1
     */
    public int train(List<String> datasets, String name, Map<String, String> args) {
        ProcessWrapper process = PythonUtils.getTracedProcess (name);
        Assert.isTrue (datasets.size () == 2, "目前只支持2个数据集输入");
        if (process == null) {
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
            if (algoService.getAlgoEntity (name) == null) {
                return -1;
            }
            return PythonUtils.runTracedScript (name, algoService.getAlgoEntity (name).getTrainSource (), map, datasets, "train");
        } else {
            return -1;
        }
    }

    public boolean predict(List<String> datasets, String name, Map<String, String> args, Integer relatedTrainModel) {
        ProcessWrapper process = PythonUtils.getTracedProcess (name);
        Assert.isTrue (datasets.size () == 2, "目前只支持2个数据集输入");
        if (process == null) {
            //获得训练的模型
            String key = ModelKeyGenerator.generateModelInfoKey (datasets.toArray (new String[]{}), name,
                    "train", null, modelInfoPrefix);
            List<ModelInfo> modelInfos = service.selectByEquals (key, ModelInfo.class, "id", String.valueOf (relatedTrainModel));
            if (modelInfos.size () != 1) {
                log.error ("找不到modelInfo，relatedTrainModel id={}", relatedTrainModel);
                return false;
            }
            ModelInfo m = modelInfos.get (0);

            Map<String, Object> map = new HashMap<> ();
            List<String> out = new ArrayList<> (datasets.size ());
            for (String s : datasets) {
                out.add (datasetService.getDatasetLocation (s));
            }
            map.put ("inputDir", m.getDataLocation ());
            map.put (datasetKey, out);
            if (algoService.getAlgoEntity (name) == null) {
                return false;
            }
            PythonUtils.runTracedScript (name, algoService.getAlgoEntity (name).getTrainSource (), map, datasets, "predict");
            return true;
        } else {
            return false;
        }
    }


    public boolean destroy(String name) {
        if (PythonUtils.getTracedProcess (name) == null) {
            log.info ("模型删除失败");
            return false;
        } else {
            PythonUtils.getTracedProcess (name).kill ();
            log.info ("模型删除成功");
            return true;
        }
    }


    public Double getPercentage(String name) {
        TracedProcessWrapper trainProcess = PythonUtils.getTracedProcess (name);
        if (trainProcess == null) {
            return null;
        }
        return trainProcess.getPercentage ();
    }

    public Double getPercentage(ModelLocationVO vo) {
        String name = ModelKeyGenerator.generateKeyWithIncId (vo.getDataset (), vo.getAlgoName (), vo.getPhase (), null, vo.getId ());
        TracedProcessWrapper trainProcess = PythonUtils.getTracedProcess (name);
        if (trainProcess == null) {
            return null;
        }
        return trainProcess.getPercentage ();
    }

    //based on latest model id
    //用于判断是否正在训练中
    public boolean contains(String name, String[] dataset, String phase, String type, boolean replaceStopped) {
        String k = getLatestKey (name, dataset, phase, type);
        if (!replaceStopped)
            return PythonUtils.getTracedProcess (k) != null;
        else
            return PythonUtils.getTracedProcess (k) != null && PythonUtils.getTracedProcess (k).getState () == State.TRAINING;
    }

    public String getLatestKey(String name, String[] dataset, String phase, String type) {
        int id = getLatestId (name, dataset, phase, type);
        return ModelKeyGenerator.generateKeyWithIncId (dataset, name, phase, type, id);
    }

    public String getKey(String name, String[] dataset, String phase, String type, Integer id) {
        return ModelKeyGenerator.generateKeyWithIncId (dataset, name, phase, type, id);
    }

    public int getLatestId(String name, String[] dataset, String phase, String type) {
        String key = ModelKeyGenerator.generateModelInfoKey (dataset, name,
                phase, type, modelInfoPrefix);
        List<ModelInfo> modelInfos = service.selectAll (key, ModelInfo.class);
        return modelInfos.size ();
    }

    public String getStatus(String name) {
        return PythonUtils.getTracedProcess (name).getState ().toString ();
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
}
