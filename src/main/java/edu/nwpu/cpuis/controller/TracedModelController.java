package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import edu.nwpu.cpuis.entity.vo.ModelLocationVO;
import edu.nwpu.cpuis.entity.vo.ModelSearchVO;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.entity.vo.PredictVO;
import edu.nwpu.cpuis.service.*;
import edu.nwpu.cpuis.service.validator.OutputVoValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Resource
    private AlgoService algoService;
    @Resource
    private Stage2OutputModelService stage2OutputModelService;
    @Resource
    private PredictionService predictionService;


//    @InitBinder
//    public void init(WebDataBinder dataBinder) {
//        dataBinder.setValidator (validator);
//    }

    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "分页查找", responseContainer = "List")
    @Deprecated
    public Response<?> getPage(@RequestBody ModelSearchVO searchVO) throws IOException {
        return ok (service.query (searchVO));
    }

    /**
     * 算法名-数据集1-数据集2-train/predict,数据集1和2必须是升序排列
     */
    @RequestMapping(value = "/output", method = {RequestMethod.GET, RequestMethod.POST}, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "获得输出", responseContainer = "List")
    public Response<?> output(@RequestBody @Validated OutputSearchVO searchVO) {
        searchVO.setId (0);

        if (!service.contains (searchVO.getAlgoName (), searchVO.getDataset (), searchVO.getPhase (), searchVO.getId ())) {
            return modelNotExists ();
        }
        AlgoEntity algoEntity = algoService.getAlgoEntity (searchVO.getAlgoName ());
        if (searchVO.getType ().equals ("statistics")) {
            //忽略trace fixme
            return ok (matrixOutputModelService.getStatistics (searchVO, false));
        } else {
            if (algoEntity.getStage ().equals ("1")) {
                return ok (matrixOutputModelService.getTracedOutput (searchVO));
            } else if (algoEntity.getStage ().equals ("2")) {
                return ok (stage2OutputModelService.getTracedOutput (searchVO));
            } else throw new CpuisException (ErrCode.WRONG_STAGE_INPUT);
        }
    }

    @RequestMapping(value = "/{name}/train", method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字\n" +
            "会返回这个模型的id")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam @Size(min = 1, max = 2) List<String> dataset) {
        dataset.sort (Comparator.naturalOrder ());
        //只能串行化训练,fixme queue?
        if (service.isTraining (name, dataset.toArray (new String[]{}), "train", true, -1)) {
            log.error ("模型正在训练中");
            return ofFailed (ErrCode.MODEL_IN_TRAINING);
        }
        Map<String, String> args = new HashMap<> ();
        int id = service.train (dataset, name, args);
        Map<String, Object> map = new HashMap<String, Object> () {
            {
                put ("id", 0);
                put ("msg", "训练开始");
            }
        };
        return ok (map);
    }


    @RequestMapping(value = "/trainingPercentage", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "获得某个模型的训练进度百分比")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> getTrainingPercentage(@RequestBody ModelLocationVO vo) {
        vo.setId (0);
        Double percentage = service.getPercentage (vo);
        if (percentage != null) {
            return ok (percentage);
        } else return modelNotExists ();
    }

    /**
     * 目前输入暂定为String
     * id 指定模型id，id可以=-1，表示最新的模型
     */
    @RequestMapping(value = "/predict", method = RequestMethod.POST)
    @ApiOperation(value = "模型预测", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> predict(@RequestPart(name = "predict") PredictVO vo,
                               @RequestPart(required = false) MultipartFile file) throws IOException {
        //TODO
        if ((vo.getInput () != null && file != null) ||
                (vo.getInput () == null && file == null)) {
            return Response.ofFailed (ErrCode.WRONG_INPUT);
        }

        vo.getDataset ().sort (Comparator.naturalOrder ());
//        PythonScriptRunner.TracedScriptOutput predict = service.predict (vo);
        return Response.ok (predictionService.predict (vo, file));
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.POST, RequestMethod.DELETE})
    @ApiOperation(value = "模型删除", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> delete(@RequestBody ModelLocationVO vo) {
        vo.setId (0);
        if (service.delete (vo)) {
            return ok ();
        } else return modelNotExists ();
    }
}
