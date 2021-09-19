package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.model.BasicModel;
import edu.nwpu.cpuis.model.DatasetService;
import edu.nwpu.cpuis.model.ModelDefinition;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Collections;

/**
 * @author fujiazheng
 */
@RestController
@RequestMapping("/model")
@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelController {
    @Resource
    private BasicModel basicModel;
    @Resource
    private ModelDefinition definition;
    @Resource
    private DatasetService fileUploadService;

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

    @GetMapping("/{name}/train")
    public Response<?> train(@PathVariable @NotBlank String name,
                             @RequestParam @NotBlank String file) {
        if (basicModel.train (Collections.singletonList (file), name)) {
            return Response.ok ("训练开始");
        } else return Response.fail ("模型已存在");
    }

    @DeleteMapping("/{name}/delete")
    public Response<?> delete(@PathVariable @NotBlank String name) {
        if (basicModel.destroy (name)) {
            return Response.ok ("模型已经删除");
        } else return Response.fail ("模型不存在");
    }

    @PostMapping("/uploadInputs")
    public Response<?> uploadInputs(@RequestPart MultipartFile file,
                                    @RequestPart("name") String datasetName) throws IOException {
        if (fileUploadService.uploadInput (file, datasetName)) {
            return Response.ok ("数据集上传成功");
        } else return Response.ok ("数据集覆盖");
    }
}
