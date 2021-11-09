package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.MatrixOutputModelService;
import edu.nwpu.cpuis.service.model.BasicModel;
import edu.nwpu.cpuis.service.model.ModelDefinition;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import edu.nwpu.cpuis.service.validator.OutputVoValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Resource
    private OutputVoValidator validator;

    //即使注册成为bean了，也得加这个
    //会覆盖默认的验证器。。,因为supports为true了
    @InitBinder
    public void init(WebDataBinder dataBinder) {
        dataBinder.setValidator (validator);
    }

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
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字;k默认为5")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class),
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam List<String> dataset) {
        if (dataset.size () != 2) {
            log.error ("dataset input err {}", dataset);
            return Response.fail ("数据集输入错误");
        }
        if (basicModel.contains (ModelKeyGenerator.generateKey (dataset.toArray (new String[]{}), name, "train", null), true)) {
            log.error ("模型正在训练中");
            return Response.fail ("模型已经存在了！");
        }
        if (fileUploadService.getDatasetLocation (dataset.get (0)) != null
                && fileUploadService.getDatasetLocation (dataset.get (1)) != null) {
            Map<String, String> args = new HashMap<> ();
            basicModel.train (dataset, name, args);//模型名字
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
            if (basicModel.contains (name, true))
                return Response.ok (basicModel.getStatus (name));
            else return Response.fail ("模型不存在");
        } catch (Exception e) {
            return Response.fail ("err " + e.getMessage ());
        }
    }

    /**
     * 算法名-数据集1-数据集2-train/predict,数据集1和2必须是升序排列
     */
    @GetMapping(value = "/output", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "获得输出")
    //加@Valid！，即使databinder弄了！
    public Response<?> output(@RequestBody @Validated OutputSearchVO searchVO) {
        try {
            if (searchVO.getType ().equals ("statistics")) {
                return Response.ok (matrixOutputModelService.getStatistics (searchVO, false));
            } else {
                return Response.ok (matrixOutputModelService.getOutput (searchVO));
            }
        } catch (Exception e) {
            e.printStackTrace ();
            return Response.fail ("err " + e.getMessage ());
        }
    }

}
