package edu.nwpu.cpuis.utils;

import edu.nwpu.cpuis.entity.DatasetEntity;
import edu.nwpu.cpuis.service.MongoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author fujiazheng
 */
@Component
@Slf4j
public class DatasetLoader {
    @Value("${dataset-prefix:dataset-}")
    public String DATASET_PREFIX;
    @Value("${dataset-replace:false}")
    private boolean replace;
    @Resource
    private MongoService<DatasetEntity> mongoService;

    /**
     * 因为数据集格式经常改变，所以要有一定容错性
     *
     * @param path 目录
     */
    public void loadDataset(String path, String datasetName) {
        try {
            final String collectionName = generateUserTraceDataCollectionName (datasetName);
            if (replace) {
                log.warn ("replace dataset {}", datasetName);
                mongoService.deleteCollection (collectionName);
            }
            if (!mongoService.collectionExists (collectionName)) {
                mongoService.createCollection (collectionName);
            } else {
                return;
            }
            File file = new File (path);
            if (!file.exists ()) {
                log.error ("数据集内容被破坏，数据集 {} 不存在", path);
                return;
            }
            Collection<File> files = FileUtils.listFiles (file, new String[]{"txt"}, false);
            files.forEach (x -> {
                String name = x.getName ().trim ();
                name = name.substring (name.indexOf ('_') + 1);
                DatasetEntity entity = new DatasetEntity ();
                entity.setId (name.substring (0, name.lastIndexOf ('.')));
                List<DatasetEntity.PathEntity> list = new ArrayList<> ();
                entity.setPath (list);
                try {
                    FileUtils.lineIterator (x)
                            .forEachRemaining (line -> {
                                        String[] split = line.trim ().split ("\\s+");
                                        //这个目前只有2个
                                        //todo
                                        DatasetEntity.PathEntity pathEntity = new DatasetEntity.PathEntity ();
                                        pathEntity.setTime (split[0]);
                                        //去除单引号，如果有的话
                                        pathEntity.setDegree (
                                                Arrays.asList (preprocess (split[1]), preprocess (split[2])));
                                        list.add (pathEntity);
                                    }
                            );
                } catch (IOException e) {
                    e.printStackTrace ();
                }
                //save
                mongoService.insert (entity, collectionName);
            });
            log.info ("insert dataset {} into mongoDB", datasetName);
        } catch (Exception e) {
            e.printStackTrace ();
            log.info ("dataset loader ERROR {},exp is {}", datasetName, e);
        }
    }

    private String preprocess(String s) {
        if (StringUtils.contains (s, '\'')) {
            return s.substring (s.indexOf ('\'') + 1, s.lastIndexOf ('\''));
        } else return "";
    }

    public String generateUserTraceDataCollectionName(String datasetName) {
        return DATASET_PREFIX + datasetName;
    }
}
