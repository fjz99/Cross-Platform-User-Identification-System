package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.DatasetEntity;
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
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

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

    @PostMapping(value = "/uploadInputs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    @GetMapping({"/trace/{dataset}/{user}", "/trace/{dataset}/"})
    @ApiOperation(value = "获得某个用户的轨迹", notes = "不输入用户的话，会返回所有数据，但是文件很大大概2-5mb的JSON文件，这个接口可以用于数据集下载")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "dataset", value = "数据集名称", required = true),
            @ApiImplicitParam(paramType = "path", name = "user", value = "用户id")
    })
    public Response<?> getTrace(@PathVariable @NotNull String dataset,
                                @PathVariable(required = false) String user) {
        if (!datasetService.checkDatasetExists (dataset)) {
            return Response.fail (String.format ("数据集%s不存在", dataset));
        }
        List<DatasetEntity> userTrace = datasetService.getUserTrace (user, dataset);
        if (userTrace == null || userTrace.size () == 0) {
            return Response.ok ("用户不存在");
        } else return Response.ok (userTrace);
    }

    @DeleteMapping(value = "/delete/{name}")
    @ApiOperation(value = "删除数据集", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "数据名字", required = true),
    })
    public Response<?> deleteAlgo(@PathVariable String name) throws IOException {
        if (datasetService.exists (name)) {
            datasetService.delete (name);
            return Response.ok ("删除成功");
        } else {
            return Response.fail ("算法不存在");
        }
    }
}
