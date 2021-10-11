package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.DatasetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
@Api(tags = "dataset", description = "数据集处理api")
public class DatasetController {
    @Resource
    private DatasetService datasetService;

    @PostMapping("/uploadInputs")
    @ApiOperation(value = "上传数据集", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, notes = "支持文件夹，压缩包格式[zip]，推荐上传zip格式")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "form", name = "file", value = "文件", required = true),
            @ApiImplicitParam(paramType = "form", name = "name", value = "数据集名称", required = true, dataType = "String")
    })
    public Response<?> uploadInputs(@RequestPart MultipartFile file,
                                    @RequestPart("name") String datasetName) throws IOException {
        if (datasetService.uploadInput (file, datasetName)) {
            return Response.ok ("数据集上传成功");
        } else return Response.ok ("数据集覆盖");
    }

    @GetMapping("/all")
    @ApiOperation(value = "获得所有数据集")
    public Response<?> all() {
        return Response.ok (datasetService.getDatasetLocation ().keySet ());
    }
}
