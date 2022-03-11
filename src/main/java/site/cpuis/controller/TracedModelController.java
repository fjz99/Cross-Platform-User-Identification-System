package site.cpuis.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.cpuis.entity.*;
import site.cpuis.entity.exception.CpuisException;
import site.cpuis.entity.vo.ModelLocationVO;
import site.cpuis.entity.vo.ModelSearchVO;
import site.cpuis.entity.vo.OutputSearchVO;
import site.cpuis.entity.vo.PredictVO;
import site.cpuis.service.*;
import site.cpuis.service.validator.OutputVoValidator;
import site.cpuis.train.ModelTrainingInfoService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.*;

import static site.cpuis.entity.Response.*;

//TODO 文件下载功能
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
    @Resource
    private ModelTrainingInfoService modelTrainingInfoService;

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
            log.error ("model already in training");
            return ofFailed (ErrCode.MODEL_IN_TRAINING);
        }
        Map<String, String> args = new HashMap<> ();
        int id = service.train (dataset, name, args);
        Map<String, Object> map = new HashMap<String, Object> () {
            {
                put ("id", 0);
                put ("msg", "training started");
            }
        };
        return ok (map);
    }


    @RequestMapping(value = "/trainingPercentage", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "获得某个模型的训练进度百分比")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    @Deprecated
    public Response<?> getTrainingPercentage(@RequestBody ModelLocationVO vo) {
        vo.setId (0);
        Double percentage = service.getPercentage (vo);
        if (percentage != null) {
            return ok (percentage);
        } else return modelNotExists ();
    }

    @RequestMapping(value = "/state", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "获得某个模型的状态")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> getModelSate(@RequestBody ModelLocationVO vo) {
        vo.setId (0);
        ModelTrainingInfo info = modelTrainingInfoService.getInfo (vo);
        if (info == null) {
            return modelNotExists ();
        } else {
            return ok (info);
        }
    }

    @RequestMapping(value = "/stage/{var}", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "获得某个模型的状态")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> getModelByStage(@PathVariable String var) {
        return ok (modelTrainingInfoService.getModels (var));
    }


    @RequestMapping(value = "/delete", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "模型删除", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    @ApiImplicitParam(paramType = "body", name = "vo", value = "定位一个模型", required = true, dataTypeClass = ModelLocationVO.class)
    public Response<?> deleteModel(@RequestBody ModelLocationVO vo) {
        vo.setId (0);
        ModelTrainingInfo info = modelTrainingInfoService.getInfo (vo);
        if (info == null) {
            return modelNotExists ();
        } else {
            modelTrainingInfoService.deleteInfo (vo);
            service.delete (vo);
            return ok ();
        }
    }


    /**
     * 目前输入暂定为String
     * id 指定模型id，id可以=-1，表示最新的模型
     */
    @RequestMapping(value = "/predict", method = RequestMethod.POST)
    @ApiOperation(value = "模型预测", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字")
    public Response<?> predict(@RequestPart(name = "predict") PredictVO vo,
                               @RequestPart(required = false) MultipartFile file) throws IOException {
        //TODO
        if (vo.getInput () != null && file != null) {
            return Response.ofFailed ("不能同时输入predict.input和file", ErrCode.WRONG_INPUT);
        }
        if (vo.getInput () == null && file == null) {
            return Response.ofFailed ("不能predict.input和file同时为空", ErrCode.WRONG_INPUT);
        }

        vo.getDataset ().sort (Comparator.naturalOrder ());
//        PythonScriptRunner.TracedScriptOutput predict = service.predict (vo);
        return Response.ok (predictionService.predict (vo, file));
    }

    /**
     * 目前输入暂定为String
     * id 指定模型id，id可以=-1，表示最新的模型
     */
    @RequestMapping(value = "/getSimilarity", method = RequestMethod.POST)
    public Response<?> getSimilarity(@RequestPart(name = "predict") OutputSearchVO vo) throws IOException {
        Arrays.sort (vo.getDataset ());
//        PythonScriptRunner.TracedScriptOutput predict = service.predict (vo);
        return Response.ok (predictionService.search (vo));
    }


    /**
     * Download outputs as csv.
     * Set search = "all" to get all data.
     */
    @RequestMapping(value = "/download", method = RequestMethod.POST)
    public void download(@RequestPart(name = "predict") PredictVO vo,
                         HttpServletResponse response) throws IOException {
        AlgoEntity entity = algoService.getAlgoEntity (vo.getAlgoName ());
        if (entity == null || !entity.getStage ().equals ("3")) {
            response.sendError (HttpServletResponse.SC_BAD_REQUEST);
        }
//        if (!entity.getStage ().equals ("3")) {
//            return ofFailed ("Algorithm's stage not equals to 3.", ErrCode.GENERIC_ERR);
//        }

        vo.getDataset ().sort (Comparator.naturalOrder ());
        predictionService.download (vo, response);
    }

}
