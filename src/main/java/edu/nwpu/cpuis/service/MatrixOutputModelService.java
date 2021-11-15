package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoOutputEntity;
import edu.nwpu.cpuis.entity.PageEntity;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


/**
 * @author fujiazheng
 * @see edu.nwpu.cpuis.service.AbstractModelService
 */
@Service
@Slf4j
public class MatrixOutputModelService extends AbstractModelService {

    public List<MongoOutputEntity> beforeSaveToMongo(List<MongoOutputEntity> list) {
        //预处理
        return null;
    }

    public @NonNull
    PageEntity<MongoOutputEntity> getOutput(@NonNull OutputSearchVO searchVO) {
        final String key = ModelKeyGenerator.generateKey (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), "output");
        log.info ("search for key {}", key);
        final int pageSize = Optional.ofNullable (searchVO.getPageSize ()).orElse (20);
        final int pageNum = Optional.ofNullable (searchVO.getPageNum ()).orElse (1);
        List<MongoOutputEntity> entities = null;
        long count = 0;
        if (searchVO.getSearchType () == null || searchVO.getSearchType ().equals ("")) {
            searchVO.setSearchType ("fulltext");
        }
        switch (searchVO.getSearchType ()) {
            case "fulltext": {
                entities = mongoOutputService.searchFullText (MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countFullText (MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
            case "regex": {
                entities = mongoOutputService.searchRegex (MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countRegex (MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
            case "normal": {
                entities = mongoOutputService.searchNormal (MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countNormal (MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
        }
        entities = preprocessTopK (entities, searchVO.getK ());
        return PageEntity.byAllDataNum (entities, (int) count, searchVO.getPageNum (), searchVO.getPageSize ());
    }

    private List<MongoOutputEntity> preprocessTopK(List<MongoOutputEntity> list, Integer topK) {
        if (topK == null) return list;
        for (MongoOutputEntity entity : list) {
            //排序，已经预处理了，所以不用了
            if (entity.getOthers ().size () > topK) {
                entity.setOthers (entity.getOthers ().subList (0, topK));
            }
        }
        return list;
    }
}
