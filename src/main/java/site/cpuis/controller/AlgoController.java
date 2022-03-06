package site.cpuis.controller;

import site.cpuis.entity.AlgoEntity;
import site.cpuis.entity.ErrCode;
import site.cpuis.entity.Response;
import site.cpuis.service.AlgoService;
import site.cpuis.service.validator.AlgoValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.Pattern;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static site.cpuis.entity.Response.ofFailed;
import static site.cpuis.entity.Response.ok;

@RestController()
@RequestMapping("/algo")
@Slf4j
@Api(tags = "algo", description = "算法管理api")
public class AlgoController {
    @Resource
    private AlgoValidator validator;
    @Resource
    private AlgoService service;

    @PostMapping(value = "/uploadInputs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传算法", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "form", name = "trainSource", value = "训练代码源文件", required = false),
            @ApiImplicitParam(paramType = "form", name = "testSource", value = "测试代码源文件", required = false),
            @ApiImplicitParam(paramType = "form", name = "predictSource", value = "预测代码源文件", required = false),
            @ApiImplicitParam(paramType = "form", name = "name", value = "算法名称", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "contact", value = "联系方式", required = false, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "description", value = "描述", required = false, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "author", value = "作者", required = false, dataType = "String"),
            @ApiImplicitParam(paramType = "form", name = "stage", value = "阶段", required = true, dataType = "String")
    })
    public Response<?> uploadInputs(@RequestPart(required = false) MultipartFile trainSource,
                                    @RequestPart(required = false) MultipartFile testSource,
                                    @RequestPart(required = false) MultipartFile predictSource,
                                    @RequestPart("name") String name,
                                    @RequestPart(required = false) String description,
                                    @RequestPart(required = false) String author,
                                    @Pattern(regexp = "\\d+") String stage,
                                    @RequestPart(required = false) String contact) throws IOException {
        AlgoEntity entity = AlgoEntity.builder ()
                .author (author)
                .contact (contact)
                .name (name)
                .description (description)
                .stage (stage)
                .time (LocalDateTime.now ())
                .build ();

        String path = service.getAlgoLocation (name);

        Response<?> response;
        if (service.exists (name)) {
//            FileUtils.deleteDirectory (new File (path));
            service.delete (name);
            log.warn ("算法文件覆盖");
            response = ok ("算法文件覆盖");
        } else {
            response = ok ("ok");
        }
        FileUtils.forceMkdir (new File (path));

        entity.setTrainSource (saveFile (trainSource, name));
        entity.setTestSource (saveFile (testSource, name));
        entity.setPredictSource (saveFile (predictSource, name));

        validator.validate (entity);

        log.debug ("get algo entity {}", entity);
        service.saveToMongoDB (entity);
        return response;
    }

    private String saveFile(MultipartFile file, String name) throws IOException {
        if (file != null) {
            String pathname = service.getAlgoLocation (name) + file.getOriginalFilename ();
            File file1 = new File (pathname);
            file.transferTo (file1);
            return pathname;
        } else return null;
    }

    @DeleteMapping(value = "/delete/{name}")
    @ApiOperation(value = "删除算法")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名字", required = true),
    })
    public Response<?> deleteAlgo(@PathVariable String name) throws IOException {
        if (service.exists (name)) {
            service.delete (name);
            return ok ("删除成功");
        } else {
            return ofFailed (ErrCode.ALGO_NOT_EXISTS);
        }
    }

    @GetMapping(value = "/get")
    @ApiOperation(value = "分页查找", responseContainer = "List")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "size", value = "页大小", required = false),
            @ApiImplicitParam(paramType = "query", name = "num", value = "页号", required = false)
    })
    public Response<?> getAlgoPage(@RequestParam(required = false, defaultValue = "20") Integer size,
                                   @RequestParam(required = false, defaultValue = "1") Integer num) throws IOException {
        return ok (service.query (size, num));
    }

    @GetMapping(value = "/getByName/{name}")
    @ApiOperation(value = "根据name查找")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名字", required = true),
    })
    public Response<?> getByName(@PathVariable String name) {
        if (service.exists (name)) {
            return ok (service.getAlgoEntity (name));
        } else {
            return ofFailed (ErrCode.ALGO_NOT_EXISTS);
        }
    }
}
