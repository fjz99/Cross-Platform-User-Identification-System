package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fujiazheng
 */
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractModelService {
    @Resource
    protected MongoService<Map> mongoMapService;

    @Resource
    protected MongoService<MongoOutputEntity> mongoOutputService;

    public @NonNull
    Map<String, Object> getStatistics(@NonNull OutputSearchVO searchVO) {
        final String key = ModelKeyGenerator.generateKey (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), searchVO.getType ());
        log.info ("search for key {}", key);
        List<Map> maps = mongoMapService.selectAll (key, Map.class);
        if (maps.size () == 0) {
            log.error ("统计数据不存在");
            return new HashMap<> ();
        }
        maps.get (0).remove ("_id");
        return maps.get (0);
    }
}
