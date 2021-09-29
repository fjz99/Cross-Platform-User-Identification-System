package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.DatasetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author fujiazheng
 */
@RestController
@RequestMapping("/dataset")
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class DatasetController {
    @Resource
    private DatasetService datasetService;

    @PostMapping("/uploadInputs")
    public Response<?> uploadInputs(@RequestPart MultipartFile file,
                                    @RequestPart("name") String datasetName) throws IOException {
        if (datasetService.uploadInput (file, datasetName)) {
            return Response.ok ("数据集上传成功");
        } else return Response.ok ("数据集覆盖");
    }

    @GetMapping("/all")
    public Response<?> all() {
        return Response.ok (datasetService.getDatasetLocation ().keySet ());
    }
}
