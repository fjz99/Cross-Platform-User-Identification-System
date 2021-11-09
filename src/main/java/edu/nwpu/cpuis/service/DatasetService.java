package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.DatasetEntity;
import edu.nwpu.cpuis.utils.DatasetLoader;
import edu.nwpu.cpuis.utils.compress.CompressService;
import edu.nwpu.cpuis.utils.compress.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author fujiazheng
 */
@Service
@Validated
@Slf4j
public class DatasetService {
    private final Map<String, String> datasetLocation = new HashMap<> (8);
    @Value("${file.input-base-location}")
    @NotNull
    private String baseLocation;
    @Value("${file.tempdir}")
    @NotNull
    private String tempDir;
    @Resource
    private List<CompressService> serviceList;
    @Resource
    private MongoService<DatasetEntity> mongoService;
    @Resource
    private DatasetLoader loader;
    @Value("${dataset-mongo-collection-name}")
    private String mongoCollection;
    @Resource
    private DatasetValidator validator;


    public boolean uploadInput(MultipartFile file, String datasetName) throws IOException {
        String path = generateDatasetLocation (datasetName);
        boolean exist = checkPath (path);
        if (exist) {
            log.warn ("数据集{}已经存在,自动覆盖", datasetName);
            datasetLocation.put (datasetName, path);
            decompress (file, path);
            loader.loadDataset (path, datasetName);
            return false;
        } else {
            datasetLocation.put (datasetName, path);
            decompress (file, path);
            loader.loadDataset (path, datasetName);
            return true;
        }
    }

    private String generateDatasetLocation(@NonNull String datasetName) {
        return baseLocation.endsWith ("/") ? baseLocation + datasetName + "/" : baseLocation + "/" + datasetName + "/";
    }

    private void decompress(MultipartFile file, String path) throws IOException {
        String originalFilename = file.getOriginalFilename ();
        file.transferTo (new File (originalFilename));
        String realPath = String.format ("%s/%s", tempDir, originalFilename);
        boolean result = false;
        for (CompressService compressService : serviceList) {
            if (compressService.support (originalFilename)) {
                log.info ("use {} to decompress", compressService.getClass ());
                result = true;
                CompressUtils.decompressZip (realPath, path);
            }
        }
        new File (realPath).delete ();
        if (!result) {
            log.error ("no CompressService can decompress file {}", originalFilename);
        }
    }

    private boolean checkPath(String path) throws IOException {
        File file = new File (path);
        if (!file.exists ()) {
            file.mkdirs ();
            return false;
        } else {
            FileUtils.forceDelete (file);
            file.mkdirs ();
            return true;
        }
    }

    public Map<String, String> getDatasetLocation() {
        return datasetLocation;
    }

    public @Nullable
    String getDatasetLocation(String name) {
        return datasetLocation.getOrDefault (name, null);
    }

    //    @PostConstruct
    public void scanDataset() {
        File path = new File (baseLocation);
        if (!path.exists ()) {
            path.mkdirs ();
        }
        for (File file : Objects.requireNonNull (new File (baseLocation).listFiles ())) {
            if (file.isDirectory ())
                datasetLocation.put (file.getName (), file.getPath ());
        }
        log.info ("auto load datasets {}", datasetLocation.entrySet ());
    }

    public boolean checkDatasetExists(String name) {
        return getDatasetLocation (name) != null;
    }

    public List<DatasetEntity> getUserTrace(String id, String datasetName) {
        final String name = loader.generateUserTraceDataCollectionName (datasetName);
        if (!mongoService.collectionExists (name)) {
            return null;
        } else if (id != null) {
            return Collections.singletonList (mongoService.selectById (id, DatasetEntity.class, name));
        } else {
            return mongoService.selectAll (name, DatasetEntity.class);
        }
    }

    public boolean exists(String name) {
        return datasetLocation.containsKey (name);
    }

    public void delete(String name) throws IOException {
        final String mongoName = loader.generateUserTraceDataCollectionName (name);
        String s = datasetLocation.get (name);
        datasetLocation.remove (name);
        FileUtils.deleteDirectory (new File (s));
        mongoService.deleteCollection (mongoName);
        //todo 删除元数据
        log.info ("delete dataset {},{},{} ok.", name, s, mongoName);
    }

    //todo
    public boolean validate(String name) throws IOException {
        return false;
    }

}
