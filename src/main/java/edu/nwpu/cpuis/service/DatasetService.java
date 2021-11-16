package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.DatasetEntity;
import edu.nwpu.cpuis.entity.DatasetManageEntity;
import edu.nwpu.cpuis.entity.PageEntity;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.validator.DatasetValidator;
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
    @Value("${dataset-nginx-dir}")
    @NotNull
    private String nginxBaseLocation;
    @NotNull
    @Value("${dataset-download-base-uri}")
    private String downloadBaseURI;
    @Value("${file.tempdir}")
    @NotNull
    private String tempDir;
    @Resource
    private List<CompressService> serviceList;
    @Resource
    private MongoService<DatasetEntity> mongoService;
    @Resource
    private MongoService<DatasetManageEntity> datasetManageEntityMongoService;
    @Resource
    private DatasetLoader loader;
    @Value("${dataset-mongo-collection-name}")
    @NotNull
    private String mongoCollection;
    @Resource
    private DatasetValidator validator;


    /**
     * @param file
     * @param datasetName
     * @param manageEntity
     * @return
     */
    public Response<?> uploadInput(MultipartFile file, String datasetName, DatasetManageEntity manageEntity) {
        try {
            manageEntity.setDownloadRelativeURI (getDownloadPath (file.getOriginalFilename ()));
            String path = generateDatasetLocation (datasetName);
            boolean exist = checkPath (path);
            delete (datasetName);//delete 依赖于dataset location map
            if (exist) {
                log.warn ("数据集{}已经存在,自动覆盖", datasetName);
                delete (datasetName);
            }
            datasetLocation.put (datasetName, path);
            decompressAndSave (file, path, manageEntity);
            if (!mongoService.collectionExists (mongoCollection)) {
                mongoService.createCollection (mongoCollection);
                mongoService.createIndex (mongoCollection, "name", true, true);
                log.warn ("DatasetManageEntity collection not exists,auto create.");
            }
            datasetManageEntityMongoService.insert (manageEntity, mongoCollection);
            Response<String> stringResponse = tryParseDataset (datasetName, path);
            if (stringResponse != null)
                return stringResponse;
            if (!exist) {
                return Response.ok ("上传成功");
            } else {
                return Response.ok ("数据集覆盖");
            }
        } catch (IOException e) {
            e.printStackTrace ();
            return Response.fail (String.format ("上传失败，ERR：%s", e.getMessage ()));
        }
    }

    private Response<String> tryParseDataset(String datasetName, String path) {
        try {
            loader.loadDataset (path, datasetName);
        } catch (Exception e) {
            return Response.ok (String.format ("上传成功\nWARN:Failed to parse dataset %s with ERR %s", datasetName, e.getMessage ()));
        }
        return null;
    }

    private String getDownloadPath(String originalFilename) {
        if (downloadBaseURI.lastIndexOf ('/') == downloadBaseURI.length () - 1) {
            return downloadBaseURI + originalFilename;
        } else return downloadBaseURI + "/" + originalFilename;
    }

    private String getFileSysPath(String downloadBaseURI) {
        if (!downloadBaseURI.contains ("/")) {
            throw new IllegalStateException ();
        }
        return nginxBaseLocation + "/" + downloadBaseURI.substring (downloadBaseURI.lastIndexOf ("/") + 1);
    }

    private String generateDatasetLocation(@NonNull String datasetName) {
        return baseLocation.endsWith ("/") ? baseLocation + datasetName + "/" : baseLocation + "/" + datasetName + "/";
    }

    /**
     * 解压并且保存到nignx映射的静态文件夹下
     *
     * @param file
     * @param path
     * @throws IOException
     */
    private void decompressAndSave(MultipartFile file, String path, DatasetManageEntity entity) throws IOException {
        String originalFilename = file.getOriginalFilename ();
        file.transferTo (new File (originalFilename));
        String realPath = String.format ("%s/%s", tempDir, originalFilename);
        boolean result = false;
        for (CompressService compressService : serviceList) {
            if (compressService.support (originalFilename)) {
                log.info ("use {} to decompressAndSave", compressService.getClass ());
                result = true;
                CompressUtils.decompressZip (realPath, path);
            }
        }
        //移动到下载位置
        File srcFile = new File (realPath);
        File destFile = new File (getFileSysPath (entity.getDownloadRelativeURI ()));
        if (destFile.exists ()) {
            destFile.delete ();
        }
        FileUtils.moveFile (srcFile, destFile);
//        srcFile.delete ();
        if (!result) {
            log.error ("no CompressService can decompressAndSave file {}", originalFilename);
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
        if (s == null) {
            log.info ("delete dataset:dataset已经被删除");
            return;
        }
        datasetLocation.remove (name);
        FileUtils.deleteDirectory (new File (s));
        mongoService.deleteCollection (mongoName);
        List<DatasetManageEntity> entities = datasetManageEntityMongoService.selectByEquals (name, DatasetManageEntity.class, mongoCollection, "name");
        if (entities.size () > 0) {
            String downloadRelativeURI = entities.get (0).getDownloadRelativeURI ();
            String fileSysPath = getFileSysPath (downloadRelativeURI);
            FileUtils.forceDelete (new File (fileSysPath));
        }
        datasetManageEntityMongoService.deleteByEqual (name, DatasetManageEntity.class, mongoCollection, "name");
        log.info ("delete dataset {},{},{} ok.", name, s, mongoName);
    }

    //todo
    public boolean validate(String name) throws IOException {
        if (datasetLocation.containsKey (name)) {
            return validator.validateFileType (datasetLocation.get (name), "txt");
        } else return false;
    }

    public String getDatasetSizePretty(long bytes) {
        final String[] f = new String[]{
                "B", "KB", "MB", "GB", "TB", "PB"
        };
        int i = 0;
        double c = bytes;
        while (c >= 1024) {
            c /= 1024;
            i++;
        }
        if (i >= f.length) {
            return String.format ("%.2fB", c);
        } else {
            return String.format ("%.2f%s", c, f[i]);
        }
    }

    public PageEntity<DatasetManageEntity> getEntityPage(Integer num, Integer size) {
        if (size == -1) {
            return PageEntity.byTotalPages (datasetManageEntityMongoService.selectAll (mongoCollection, DatasetManageEntity.class),
                    -1, -1, -1);
        } else {
            return PageEntity.byAllDataNum (datasetManageEntityMongoService.selectList (mongoCollection, DatasetManageEntity.class, num, size),
                    (int) datasetManageEntityMongoService.countAll (mongoCollection, DatasetManageEntity.class), num, size);
        }
    }

    public DatasetManageEntity getEntity(String name) {
        List<DatasetManageEntity> name1 = datasetManageEntityMongoService.selectByEquals (mongoCollection, DatasetManageEntity.class, "name", name);
        if (name1.size () == 0) {
            return null;
        } else return name1.get (0);
    }
}
