package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.MatrixOutputModelService;
import edu.nwpu.cpuis.service.model.BasicModel;
import edu.nwpu.cpuis.service.validator.OutputVoValidator;
import edu.nwpu.cpuis.train.State;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.nwpu.cpuis.entity.Response.*;

@RestController
@RequestMapping("/model")
@Slf4j
@Api(tags = "model", description = "模型api")
public class ModelController {
    @Resource
    private BasicModel basicModel;
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


    @GetMapping("/{name}/trainingPercentage")
    @ApiOperation(value = "获得某个模型的训练进度百分比")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> getTrainingPercentage(@PathVariable @NotBlank String name) {
        Double percentage = basicModel.getPercentage (name);
        if (percentage != null) {
            return ok (percentage);
        } else return modelNotExists ();
    }

    @GetMapping("/{name}/stop")
    @ApiOperation(value = "终止某个模型训练过程")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> stop(@PathVariable @NotBlank String name) {
        State state = basicModel.getState (name);
        if (state == null) {
            return modelNotExists ();
        }
        //stop方法会进行验证
//        if (state != State.TRAINING) {
//            return ofFailed (state, ErrCode.MODEL_ALREADY_STOPPED);
//        }
        if (basicModel.stopTrain (name)) {
            return ok ();
        } else {
            return genericErr ();
        }
    }

    @DeleteMapping("/{name}/delete")
    @ApiOperation(value = "删除模型")
    @ApiImplicitParam(paramType = "path", name = "name", value = "模型运行id", required = true, dataTypeClass = String.class)
    public Response<?> delete(@PathVariable @NotBlank String name) {
        if (basicModel.destroy (name)) {
            return ok ();
        } else return modelNotExists ();
    }

    @GetMapping("/{name}/train")
    @ApiOperation(value = "模型训练", notes = "注意数据集名称参数dataset，只能选定2个数据集，而且这两个数据集的名字必须是上传的名字;k默认为5")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名称", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(paramType = "query", name = "dataset", value = "数据集名称", required = true, dataTypeClass = List.class, allowMultiple = true),
    })
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam List<String> dataset) {
        if (dataset.size () != 2 || dataset.get (0).equalsIgnoreCase (dataset.get (1))) {
            log.error ("dataset input err {}", dataset);
            return ofFailed (ErrCode.WRONG_DATASET_INPUT);
        }
        if (basicModel.contains (ModelKeyGenerator.generateKey (dataset.toArray (new String[]{}), name, "getDaemon", null), true)) {
            log.error ("模型正在训练中");
            return ofFailed (ErrCode.MODEL_IN_TRAINING);
        }
        if (fileUploadService.getDatasetLocation (dataset.get (0)) != null
                && fileUploadService.getDatasetLocation (dataset.get (1)) != null) {
            Map<String, String> args = new HashMap<> ();
            basicModel.train (dataset, name, args);//模型名字
            return ok ("训练开始");
        } else {
            log.error ("dataset input err {}", dataset);
            return ofFailed (ErrCode.WRONG_DATASET_INPUT);
        }
    }

    /**
     * 算法名-数据集1-数据集2-getDaemon/predict,数据集1和2必须是升序排列
     */
    @RequestMapping(value = "/output", consumes = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "获得输出", responseContainer = "List")
    //加@Valid！，即使databinder弄了！
    public Response<?> output(@RequestBody @Validated OutputSearchVO searchVO) {
        searchVO.setId (-1);
        if (searchVO.getType ().equals ("statistics")) {
            return Response.ok (matrixOutputModelService.getStatistics (searchVO, false));
        } else {
            return Response.ok (matrixOutputModelService.getOutput (searchVO));
        }
    }

}
