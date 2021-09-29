package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.MatrixOutputModelService;
import edu.nwpu.cpuis.service.model.BasicModel;
import edu.nwpu.cpuis.service.model.ModelDefinition;
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
    public Response<?> getInfo(@PathVariable("name") @NotBlank String name) {
        if (definition.getDefinition ().containsKey (name)) {
            return Response.ok (definition.getDefinition ().get (name));
        } else {
            return Response.fail ("no content");
        }
    }

    @GetMapping("/stage/{id}")
    public Response<?> getByStage(@PathVariable @Range(min = 1, max = 4) int id) {
        return Response.ok (definition.getByStage (id));
    }

    @GetMapping("/all")
    public Response<?> getAll() {
        return Response.ok (definition.getAll ());
    }

    @GetMapping("/{name}/trainingPercentage")
    public Response<?> getTrainingPercentage(@PathVariable @NotBlank String name) {
        Double percentage = basicModel.getPercentage (name);
        if (percentage != null) {
            return Response.ok (percentage);
        } else return Response.fail ("模型不存在");
    }

    @DeleteMapping("/{name}/delete")
    public Response<?> delete(@PathVariable @NotBlank String name) {
        if (basicModel.destroy (name)) {
            return Response.ok ("模型已经删除");
        } else return Response.fail ("模型不存在");
    }

    @PostMapping("/{name}/train")
    public Response<?> train(@PathVariable String name,
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
    public Response<?> status(@PathVariable String name) {
        try {
            if (basicModel.contains (name))
                return Response.ok (basicModel.getStatus (name));
            else return Response.fail ("模型不存在");
        } catch (Exception e) {
            return Response.fail ("err " + e.getMessage ());
        }
    }

    @GetMapping("/{key}/{type:output|metadata}")
    public Response<?> output(@PathVariable String key,
                              @PathVariable String type,
                              @RequestParam(value = "id", required = false)
                              @Size(max = 2, message = "id参数不合规范") String[] id) {
        try {
            switch (type) {
                case "output": {
                    if (id == null) {
                        return Response.ok (matrixOutputModelService.getAllMatchedUsers (key));
                    } else {
                        Map<String, Object> result = new HashMap<> ();
                        Stream.of (id).forEach (x -> {
                            Object matchedUsers = matrixOutputModelService.getMatchedUsers (x, key);
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
