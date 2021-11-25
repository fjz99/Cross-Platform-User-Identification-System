package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.vo.ModelLocationVO;
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
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.nwpu.cpuis.entity.Response.*;


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
        return ok (service.query (searchVO));
    }

    /**
     * 算法名-数据集1-数据集2-getDaemon/predict,数据集1和2必须是升序排列
     */
    @RequestMapping(value = "/output", consumes = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "获得输出", responseContainer = "List")
    public Response<?> output(@RequestBody @Validated OutputSearchVO searchVO) {
        if (!service.contains (searchVO.getAlgoName (), searchVO.getDataset (), searchVO.getPhase (), searchVO.getId ())) {
            return modelNotExists ();
        }
        if (searchVO.getType ().equals ("statistics")) {
            //忽略trace fixme
            return ok (matrixOutputModelService.getStatistics (searchVO, false));
        } else {
            return ok (matrixOutputModelService.getTracedOutput (searchVO));
        }
    }

    @GetMapping("/{name}/train")
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字\n" +
            "会返回这个模型的id")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam @Size(min = 2, max = 2) List<String> dataset) {
        dataset.sort (Comparator.naturalOrder ());
        if (dataset.size () != 2) {
            log.error ("dataset input err {}", dataset);
            return ofFailed (ErrCode.WRONG_DATASET_INPUT);
        }
        if (service.isTraining (name, dataset.toArray (new String[]{}), "getDaemon", null, true)) {
            log.error ("模型正在训练中");
            return ofFailed (ErrCode.MODEL_IN_TRAINING);
        }
        if (fileUploadService.getDatasetLocation (dataset.get (0)) != null
                && fileUploadService.getDatasetLocation (dataset.get (1)) != null) {
            Map<String, String> args = new HashMap<> ();
            int id = service.train (dataset, name, args);
            Map<String, Object> map = new HashMap<String, Object> () {
                {
                    put ("id", id);
                    put ("msg", "训练开始");
                }
            };
            return ok (map);
        } else {
            log.error ("dataset input err {}", dataset);
            return ofFailed (ErrCode.WRONG_DATASET_INPUT);
        }
    }

    //todo
    @GetMapping("/trainingPercentage")
    @ApiOperation(value = "获得某个模型的训练进度百分比")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> getTrainingPercentage(@RequestBody ModelLocationVO vo) {
        Double percentage = service.getPercentage (vo);
        if (percentage != null) {
            return ok (percentage);
        } else return modelNotExists ();
    }

    //todo
    @GetMapping("/{name}/predict")
    @ApiOperation(value = "模型预测", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> predict(@PathVariable @NotBlank String name,
                               @RequestParam @Size(min = 2, max = 2) List<String> dataset) {
        return null;
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "模型删除", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> delete(@RequestBody ModelLocationVO vo) {
        if (service.delete (vo)) {
            return ok ();
        } else return modelNotExists ();
    }
}
