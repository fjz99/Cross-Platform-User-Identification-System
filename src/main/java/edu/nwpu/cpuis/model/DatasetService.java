package edu.nwpu.cpuis.model;

import edu.nwpu.cpuis.utils.compress.CompressService;
import edu.nwpu.cpuis.utils.compress.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Validated
@Slf4j
public class DatasetService {
    private final Map<String, String> datasetLocation = new HashMap<> ();
    @Value("${file.input-base-location}")
    @NotNull
    private String baseLocation;
    @Value("${file.tempdir}")
    @NotNull
    private String tempDir;
    @Resource
    private List<CompressService> serviceList;

    public boolean uploadInput(MultipartFile file, String datasetName) throws IOException {
        String path = generateDatasetLocation (datasetName);
        boolean exist = checkPath (path);
        if (exist) {
            log.warn ("数据集{}已经存在,自动覆盖", datasetName);
            datasetLocation.put (datasetName, path);
            decompress (file, path);
            return false;
        } else {
            datasetLocation.put (datasetName, path);
            decompress (file, path);
            return true;
        }
    }

    private String generateDatasetLocation(@NonNull String datasetName) {
        return baseLocation.endsWith ("/") ? baseLocation + datasetName + "/" : baseLocation + "/" + datasetName + "/";
    }

    private boolean decompress(MultipartFile file, String path) throws IOException {
        file.transferTo (new File (file.getOriginalFilename ()));
        String realPath = String.format ("%s/%s", tempDir, file.getOriginalFilename ());
        boolean result = false;
        for (CompressService compressService : serviceList) {
            if (compressService.support (file.getOriginalFilename ())) {
                log.info ("use {} to decompress", compressService.getClass ());
                result = true;
                CompressUtils.decompressZip (realPath, path);
            }
        }
        new File (realPath).delete ();
        if (!result) {
            log.error ("no CompressService can decompress file {}", file.getOriginalFilename ());
        }
        return result;
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
}
