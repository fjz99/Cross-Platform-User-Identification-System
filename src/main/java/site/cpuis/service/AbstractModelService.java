package site.cpuis.service;

import site.cpuis.entity.MongoOutputEntity;
import site.cpuis.entity.vo.OutputSearchVO;
import site.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@SuppressWarnings({"rawtypes"})
public abstract class AbstractModelService {
    @Resource
    protected DataBaseService<Map> mongoMapService;

    @Resource
    protected DataBaseService<MongoOutputEntity> mongoOutputService;

    public @NonNull
    List<Map> getStatistics(@NonNull OutputSearchVO searchVO, boolean latest) {
        final String key = ModelKeyGenerator.generateKey (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), searchVO.getType ());
        log.info ("search for key {}", key);
        List<Map> list = mongoMapService.selectAll (key, Map.class);
        if (list.size () == 0) {
            log.error ("统计数据不存在");
            return Collections.emptyList ();
        }
        if (latest) {
            return list.subList (list.size () - 1, list.size ());
        } else {
            return list;
        }
    }
}
