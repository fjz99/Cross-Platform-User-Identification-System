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

@Service
@Validated
@Slf4j
public class DatasetService {
    private final Map<String, String> datasetLocation = new HashMap<> (8);
    @Value("${file.input-base-location}")
    @NotNull
    public String baseLocation;
    @Value("${dataset-nginx-dir}")
    @NotNull
    public String nginxBaseLocation;
    @NotNull
    @Value("${dataset-download-base-uri}")
    public String downloadBaseURI;
    @Value("${file.tempdir}")
    @NotNull
    public String tempDir;
    @Value("${dataset-mongo-collection-name}")
    @NotNull
    public String mongoCollection;
    @Resource
    private List<CompressService> serviceList;
    @Resource
    private MongoService<DatasetEntity> mongoService;
    @Resource
    private MongoService<DatasetManageEntity> datasetManageEntityMongoService;
    @Resource
    private DatasetLoader loader;
    @Resource
    private DatasetValidator validator;

    public List<DatasetManageEntity> getAll() {
        return datasetManageEntityMongoService.selectAll (mongoCollection, DatasetManageEntity.class);
    }

    public Response<?> uploadInput(MultipartFile file, String datasetName, DatasetManageEntity manageEntity) throws IOException {
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
            delete (datasetName);
            return Response.of (String.format ("上传失败，ERR：%s", e.getMessage ()), false, Response.ErrCode.DATASET_VALIDATION_FAILED);
        }
    }

    //依赖于map，因为要查找entity
    private Response<String> tryParseDataset(String datasetName, String path) throws IOException {
        try {
            //验证文件类型
            DatasetValidator.FileTypeValidatorOutput output = validator.validateFileType (datasetLocation.get (datasetName), "txt");
            if (!output.isOk ()) {
                log.warn ("数据集验证失败，必须是txt,造成异常的文件名为{}", output.getFailedFile ());
                delete (datasetName);
                return Response.of (String.format ("上传失败;WARN:数据集 %s 验证失败,数据集格式必须是txt,造成异常的文件名为'%s'",
                        datasetName, output.getFailedFile ()), false, Response.ErrCode.DATASET_VALIDATION_FAILED);
            }
            loader.loadDataset (path, datasetName);
        } catch (Exception e) {
            e.printStackTrace ();
            delete (datasetName);
            return Response.of (String.format ("上传成功\nWARN:数据集 %s 验证失败,ERR %s", datasetName, e.getMessage ()),
                    true, Response.ErrCode.DATASET_VALIDATION_FAILED);
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
        file.transferTo (new File (originalFilename)); //此时，E：tempdir中就有了xx.zip
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

    /**
     * 这个方法要求datasetLocation map中存在数据才能删除
     *
     * @param name
     * @throws IOException
     */
    public void delete(String name) throws IOException {
        final String mongoName = loader.generateUserTraceDataCollectionName (name);
        String s = datasetLocation.get (name);
        if (s == null) {
            log.warn ("delete dataset:dataset {} 已经被删除", name);
            return;
        }
        datasetLocation.remove (name);
        FileUtils.deleteDirectory (new File (s));
        mongoService.deleteCollection (mongoName);
        List<DatasetManageEntity> entities = datasetManageEntityMongoService.selectByEquals (mongoCollection, DatasetManageEntity.class, "name", name);
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
            return validator.validateFileType (datasetLocation.get (name), "txt").isOk ();
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
