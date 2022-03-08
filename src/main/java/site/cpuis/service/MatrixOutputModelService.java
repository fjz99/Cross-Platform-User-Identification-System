package site.cpuis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import site.cpuis.entity.MongoOutputEntity;
import site.cpuis.entity.PageEntity;
import site.cpuis.entity.vo.OutputSearchVO;
import site.cpuis.utils.CsvExportUtil;
import site.cpuis.utils.ModelKeyGenerator;

import javax.servlet.http.HttpServletResponse;
import java.util.*;


/**
 * @see AbstractModelService
 */
@Service
@Slf4j
public class MatrixOutputModelService extends AbstractModelService {

    public static final String USER_NAME = "userName";

    public List<MongoOutputEntity> beforeSaveToMongo(List<MongoOutputEntity> list) {
        //预处理
        return null;
    }

    public void transferFile(String key, HttpServletResponse response) {
        try {
            List<MongoOutputEntity> entities = mongoOutputService.selectAll (key, MongoOutputEntity.class);
            String resKey = "id1,id2";
            String titles = "id1,id2";  // 设置表头
            List<Map<String, Object>> maps = new ArrayList<> ();
            for (MongoOutputEntity entity : entities) {
                HashMap<String, Object> e = new HashMap<> ();
                maps.add (e);
            }
            CsvExportUtil.responseSetProperties ("output_", response);
            CsvExportUtil.doExport (maps, titles, resKey, response.getOutputStream ());
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    /**
     * Set pageNum = pageSize = -1 to get all data.
     */
    private @NonNull
    PageEntity<MongoOutputEntity> getOutput0(@NonNull OutputSearchVO searchVO, String key) {
        log.info ("search for key {}", key);

        int pageSize = Optional.ofNullable (searchVO.getPageSize ()).orElse (20);
        int pageNum = Optional.ofNullable (searchVO.getPageNum ()).orElse (1);

        //检查是否存在数据 bug-fix
        long l = mongoOutputService.countAll (key, MongoOutputEntity.class);
        if (l == 0) {
            return PageEntity.byTotalPages (new ArrayList<> (), 0, 1, searchVO.getPageSize ());
        }
        if (pageSize == -1) {
            pageNum = 1;
            pageSize = (int) l;
        }


        List<MongoOutputEntity> entities = null;
        long count = 0;
        if (searchVO.getSearchType () == null || searchVO.getSearchType ().equals ("")) {
            searchVO.setSearchType ("fulltext");
        }
        switch (searchVO.getSearchType ()) {
            case "all": {
                entities = mongoOutputService.selectAll (key, MongoOutputEntity.class);
                count = l;
                break;
            }
            case "fulltext": {
                entities = mongoOutputService.searchFullText (MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countFullText (MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
            case "regex": {
                entities = mongoOutputService.searchRegex (USER_NAME, MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countRegex (USER_NAME, MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
            case "normal": {
                entities = mongoOutputService.searchNormal (USER_NAME, MongoOutputEntity.class, key, searchVO.getSearchText (), pageNum, pageSize);
                count = mongoOutputService.countNormal (USER_NAME, MongoOutputEntity.class, key, searchVO.getSearchText ());
                break;
            }
        }
        entities = preprocessTopK (entities, searchVO.getK ());
        return PageEntity.byAllDataNum (entities, (int) count, pageNum, pageSize);
    }

    public @NonNull
    PageEntity<MongoOutputEntity> getOutput(@NonNull OutputSearchVO searchVO) {
        final String key = ModelKeyGenerator.generateKey (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), "output");
        return getOutput0 (searchVO, key);
    }

    public @NonNull
    PageEntity<MongoOutputEntity> getTracedOutput(@NonNull OutputSearchVO searchVO) {
        final String key = ModelKeyGenerator.generateKeyWithIncId (searchVO.getDataset (),
                searchVO.getAlgoName (), searchVO.getPhase (), "output", searchVO.getId ());
        return getOutput0 (searchVO, key);
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
