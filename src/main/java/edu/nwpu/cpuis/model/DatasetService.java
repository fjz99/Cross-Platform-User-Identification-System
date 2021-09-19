package edu.nwpu.cpuis.model;

import edu.nwpu.cpuis.utils.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    private void decompress(MultipartFile file, String path) throws IOException {
        file.transferTo (new File (file.getOriginalFilename ()));
        String realPath = String.format ("%s/%s", tempDir, file.getOriginalFilename ());
        CompressUtils.decompressZip (realPath, path);
        new File (realPath).delete ();
    }

    private boolean checkPath(String path) {
        File file = new File (path);
        if (!file.exists ()) {
            file.mkdirs ();
            return false;
        } else {
            file.delete ();
            file.mkdirs ();
            return true;
        }
    }

    public Map<String, String> getDatasetLocation() {
        return datasetLocation;
    }
}
