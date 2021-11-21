package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.vo.ModelSearchVO;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.MatrixOutputModelService;
import edu.nwpu.cpuis.service.TracedModelService;
import edu.nwpu.cpuis.service.validator.OutputVoValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/model/traced/")
@Slf4j
@Api(tags = "tracedModel", description = "模型api", hidden = true)
public class TracedModelController {
    @Resource
    private DatasetService fileUploadService;
    @Resource
    private MatrixOutputModelService matrixOutputModelService;
    @Resource
    private OutputVoValidator validator;
    @Resource
    private TracedModelService service;


//    @InitBinder
//    public void init(WebDataBinder dataBinder) {
//        dataBinder.setValidator (validator);
//    }

    @GetMapping(value = "/get")
    @ApiOperation(value = "分页查找", responseContainer = "List")
    public Response<?> getPage(@RequestBody ModelSearchVO searchVO) throws IOException {
        return Response.ok (service.query (searchVO));
    }

    /**
     * 算法名-数据集1-数据集2-train/predict,数据集1和2必须是升序排列
     */
    @RequestMapping(value = "/output", consumes = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "获得输出", responseContainer = "List")
    public Response<?> output(@RequestBody @Validated OutputSearchVO searchVO) {
        try {
            if (searchVO.getType ().equals ("statistics")) {
                //忽略trace
                return Response.ok (matrixOutputModelService.getStatistics (searchVO, false));
            } else {
                return Response.ok (matrixOutputModelService.getTracedOutput (searchVO));
            }
        } catch (Exception e) {
            e.printStackTrace ();
            return Response.fail ("err " + e.getMessage ());
        }
    }

    @GetMapping("/{name}/train")
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字;k默认为5")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam List<String> dataset) {
        dataset.sort (Comparator.naturalOrder ());
        if (dataset.size () != 2) {
            log.error ("dataset input err {}", dataset);
            return Response.fail ("数据集输入错误");
        }
        if (service.contains (name, dataset.toArray (new String[]{}), "train", null, true)) {
            log.error ("模型正在训练中");
            return Response.fail ("模型已经存在了！");
        }
        if (fileUploadService.getDatasetLocation (dataset.get (0)) != null
                && fileUploadService.getDatasetLocation (dataset.get (1)) != null) {
            Map<String, String> args = new HashMap<> ();
            service.train (dataset, name, args);
            return Response.ok ("训练开始");
        } else {
            log.error ("dataset input err {}", dataset);
            return Response.fail ("数据集输入错误");
        }
    }

//    @GetMapping("/{name}/trainingPercentage")
//    @ApiOperation(value = "获得某个模型的训练进度百分比")
//    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
//    public Response<?> getTrainingPercentage(@PathVariable @NotBlank String name) {
//        Double percentage = basicModel.getPercentage (name);
//        if (percentage != null) {
//            return Response.ok (percentage);
//        } else return Response.fail ("模型不存在");
//    }
}
