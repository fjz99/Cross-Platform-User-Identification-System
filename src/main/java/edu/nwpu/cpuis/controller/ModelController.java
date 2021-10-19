package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.MatrixOutputModelService;
import edu.nwpu.cpuis.service.model.BasicModel;
import edu.nwpu.cpuis.service.model.ModelDefinition;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author fujiazheng
 */
@RestController
@RequestMapping("/model")
@Slf4j
@Api(tags = "model", description = "模型api")
@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelController {
    @Resource
    private BasicModel basicModel;
    @Resource
    private ModelDefinition definition;
    @Resource
    private DatasetService fileUploadService;
    @Resource
    private MatrixOutputModelService matrixOutputModelService;

    @GetMapping("{name}/info")
    @ApiOperation(value = "获得模型的详细信息", notes = "传入模型的key，即'算法-数据集1-数据集2-train/predict'")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型名称", required = true, dataType = "String")
    public Response<?> getInfo(@PathVariable("name") @NotBlank String name) {
        if (definition.getDefinition ().containsKey (name)) {
            return Response.ok (definition.getDefinition ().get (name));
        } else {
            return Response.fail ("no content");
        }
    }

    @GetMapping("/stage/{id}")
    @ApiOperation(value = "获得某个阶段的所有算法")
    @ApiImplicitParam(paramType = "path", name = "id", value = "阶段", required = true, dataTypeClass = Integer.class)
    public Response<?> getByStage(@PathVariable @Range(min = 1, max = 4) int id) {
        return Response.ok (definition.getByStage (id));
    }

    @GetMapping("/all")
    @ApiOperation(value = "获得所有模型信息")
    public Response<?> getAll() {
        return Response.ok (definition.getAll ());
    }

    @GetMapping("/{name}/trainingPercentage")
    @ApiOperation(value = "获得某个模型的训练进度百分比")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> getTrainingPercentage(@PathVariable @NotBlank String name) {
        Double percentage = basicModel.getPercentage (name);
        if (percentage != null) {
            return Response.ok (percentage);
        } else return Response.fail ("模型不存在");
    }

    @DeleteMapping("/{name}/delete")
    @ApiOperation(value = "删除模型")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> delete(@PathVariable @NotBlank String name) {
        if (basicModel.destroy (name)) {
            return Response.ok ("模型已经删除");
        } else return Response.fail ("模型不存在");
    }

    @GetMapping("/{name}/train")
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class)
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam List<String> dataset) {
        if (dataset.size () != 2) {
            log.error ("dataset input err {}", dataset);
            return Response.fail ("数据集输入错误");
        }
        if (fileUploadService.getDatasetLocation (dataset.get (0)) != null
                && fileUploadService.getDatasetLocation (dataset.get (1)) != null) {
            basicModel.train (dataset, name);//模型名字
            return Response.ok ("训练开始");
        } else {
            log.error ("dataset input err {}", dataset);
            return Response.fail ("数据集输入错误");
        }
    }

    @GetMapping("/{name}/status")
    @ApiOperation(value = "获得模型状态")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> status(@PathVariable @NotBlank String name) {
        try {
            if (basicModel.contains (name))
                return Response.ok (basicModel.getStatus (name));
            else return Response.fail ("模型不存在");
        } catch (Exception e) {
            return Response.fail ("err " + e.getMessage ());
        }
    }

    /**
     * @param key 算法名-数据集1-数据集2-train/predict,数据集1和2必须是升序排列
     */
    @GetMapping("/{key}/{type:output|metadata}")
    @ApiOperation(value = "获得模型输出", notes = "注意参数，支持output和metadata两种，metadata会返回模型输出的一些统计信息，比如运行时间；\n" +
            "而output就是返回输出内容")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "key", value = "模型运行id", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "path", name = "type", value = "查询类型，为output和metadata其中的一个", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "id", value = "output模式下，指定查询的用户id，若不指定，则为查询所有用户",
                    dataTypeClass = String[].class),
            @ApiImplicitParam(paramType = "query", name = "reverse", value = "output模式下，指定是否反向查询，具体细节QQ询问",
                    dataTypeClass = Boolean.class, defaultValue = "false")
    })
    public Response<?> output(@PathVariable @NotBlank String key,
                              @PathVariable String type,
                              @RequestParam(value = "id", required = false)
                              @Size(max = 2, message = "id参数不合规范") String[] id,
                              @RequestParam(value = "reverse", required = false, defaultValue = "false")
                                      Boolean reverse) {
        try {
            switch (type) {
                case "output": {
                    if (id == null) {
                        return Response.ok (matrixOutputModelService.getAllMatchedUsers (key));
                    } else {
                        Map<String, Object> result = new HashMap<> ();
                        Stream.of (id).forEach (x -> {
                            Object matchedUsers = matrixOutputModelService.getMatchedUsers (x, key, reverse);
                            List<Object> changed = ((List<Object>) matchedUsers);
                            result.put (x, changed);
                        });
                        return Response.ok (result);
                    }
                }
                case "metadata": {
                    return Response.ok (matrixOutputModelService.getMetadata (key));
                }
                default:
                    return Response.fail ("err server failed");
            }
        } catch (Exception e) {
            e.printStackTrace ();
            return Response.fail ("err " + e.getMessage ());
        }
    }

}
