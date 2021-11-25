package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.DatasetEntity;
import edu.nwpu.cpuis.entity.DatasetManageEntity;
import edu.nwpu.cpuis.entity.ErrCode;
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
import javax.servlet.http.Part;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static edu.nwpu.cpuis.entity.Response.*;

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
            @ApiImplicitParam(paramType = "form", name = "file", value = "文件", required = true, dataTypeClass = Part.class),
            @ApiImplicitParam(paramType = "form", name = "name", value = "数据集名称", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "description", value = "描述", required = false, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "author", value = "作者", required = false, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "contact", value = "联系方式", required = false, dataType = "String")
    })
    public Response<?> uploadInputs(@RequestPart MultipartFile file,
                                    @RequestPart("name") String datasetName,
                                    @RequestPart(required = false) String description,
                                    @RequestPart(required = false) String author,
                                    @RequestPart(required = false) String contact) throws IOException {
        DatasetManageEntity manageEntity = DatasetManageEntity
                .builder ()
                .author (author)
                .description (description)
                .contact (contact)
                .time (LocalDateTime.now ())
                .name (datasetName)
                .size (datasetService.getDatasetSizePretty (file.getSize ()))
                .build ();
        return datasetService.uploadInput (file, datasetName, manageEntity);
    }

    @GetMapping("/all")
    @ApiOperation(value = "获得所有数据集")
    public Response<?> all() {
        return ok (datasetService.getAll ());
    }

    @GetMapping({"/trace/{dataset}/{user}", "/trace/{dataset}/"})
    @ApiOperation(value = "获得某个用户的轨迹", notes = "不输入用户的话，会返回所有数据，但是文件很大大概2-5mb的JSON文件，这个接口可以用于数据集下载")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "dataset", value = "数据集名称", required = true),
            @ApiImplicitParam(paramType = "path", name = "user", value = "用户id")
    })
    public Response<?> getTrace(@PathVariable @NotNull String dataset,
                                @PathVariable(required = false) String user) {
        if (!datasetService.exists (dataset)) {
            return ofFailed (String.format ("数据集%s不存在", dataset), ErrCode.DATASET_NOT_EXISTS);
        }
        List<DatasetEntity> userTrace = datasetService.getUserTrace (user, dataset);
        if (userTrace == null || userTrace.size () == 0) {
            return ok ("用户不存在");
        } else return ok (userTrace);
    }

    @DeleteMapping(value = "/delete/{name}")
    @ApiOperation(value = "删除数据集")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "数据名字", required = true),
    })
    public Response<?> deleteDataset(@PathVariable String name) throws IOException {
        if (datasetService.exists (name)) {
            datasetService.delete (name);
            return ok ();
        } else {
            return ofFailed (ErrCode.DATASET_NOT_EXISTS);
        }
    }

    @GetMapping(value = "/get")
    @ApiOperation(value = "分页查找", responseContainer = "List")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "size", value = "页大小", required = false),
            @ApiImplicitParam(paramType = "query", name = "num", value = "页号", required = false)
    })
    public Response<?> getDatasetPage(@RequestParam(required = false, defaultValue = "20") Integer size,
                                      @RequestParam(required = false, defaultValue = "1") Integer num) {
        return ok (datasetService.getEntityPage (num, size));
    }

    @GetMapping(value = "/getByName/{name}")
    @ApiOperation(value = "根据name查找")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "名字", required = true),
    })
    public Response<?> getDatasetPage(@PathVariable String name) {
        if (datasetService.exists (name)) {
            return ok (datasetService.getEntity (name));
        } else {
            return ofFailed (ErrCode.DATASET_NOT_EXISTS);
        }
    }
}
