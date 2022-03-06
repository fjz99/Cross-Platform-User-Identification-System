package org.cpuis.service;

import org.cpuis.entity.PageEntity;
import org.cpuis.entity.vo.OutputSearchVO;
import org.cpuis.train.output.Stage2Output;
import org.cpuis.utils.ModelKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * @see AbstractModelService
 */
@Service
@Slf4j
public class Stage2OutputModelService {

    public static final String NAME = "name";
    @Resource
    protected MongoService<Map> mongoMapService;

    @Resource
    protected MongoService<Stage2Output> mongoOutputService;

    public List<Stage2Output> beforeSaveToMongo(List<Stage2Output> list) {
        //预处理
        return null;
    }

    private @NonNull
    PageEntity<Stage2Output> getOutput0(@NonNull OutputSearchVO searchVO, String key) {
        log.info ("search for key {}", key);
        final int pageSize = Optional.ofNullable (searchVO.getPageSize ()).orElse (20);
        final int pageNum = Optional.ofNullable (searchVO.getPageNum ()).orElse (1);

        //检查是否存在数据 bug-fix
        long l = mongoOutputService.countAll (key, Stage2Output.class);
        if (l == 0) {
            return PageEntity.byTotalPages (new ArrayList<> (), 0, 1, pageSize);
        }

        List<Stage2Output> entities = null;
        long count = 0;
        if (searchVO.getSearchType () == null || searchVO.getSearchType ().equals ("")) {
            searchVO.setSearchType ("fulltext");
        }
        switch (searchVO.getSearchType ()) {
            case "fulltext": {
                entities = mongoOutputService.searchFullText (Stage2Output.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countFullText (Stage2Output.class, key, searchVO.getSearchText ());
                break;
            }
            case "regex": {
                entities = mongoOutputService.searchRegex (NAME, Stage2Output.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countRegex (NAME, Stage2Output.class, key, searchVO.getSearchText ());
                break;
            }
            case "normal": {
                entities = mongoOutputService.searchNormal (NAME, Stage2Output.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countNormal (NAME, Stage2Output.class, key, searchVO.getSearchText ());
                break;
            }
            case "all": {
                entities = mongoOutputService.selectAll (key, Stage2Output.class);
                count = mongoOutputService.countAll (key, Stage2Output.class);
                break;
            }
        }
        return PageEntity.byAllDataNum (entities, (int) count, pageNum, pageSize);
    }

    public @NonNull
    PageEntity<Stage2Output> getTracedOutput(@NonNull OutputSearchVO searchVO) {
        final String key = ModelKeyGenerator.generateKeyWithIncId (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), "output", searchVO.getId ());
        return getOutput0 (searchVO, key);
    }
}
