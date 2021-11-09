package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.AlgoService;
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

@RestController()
@RequestMapping("/algo")
@Slf4j
public class AlgoController {
    @Resource
    private AlgoService service;

    @PostMapping(value = "/uploadInputs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传某个算法文件", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, notes = "支持文件夹，压缩包格式[zip]，推荐上传zip格式\n需要整体上传")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "form", name = "file", value = "文件", required = true),
            @ApiImplicitParam(paramType = "form", name = "name", value = "算法名称", required = true, dataType = "String")
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
        if (!service.exists (name)) {
            FileUtils.deleteDirectory (new File (path));
            log.warn ("算法文件覆盖");
            response = Response.ok ("算法文件覆盖");
        } else {
            response = Response.ok ("ok");
        }
        FileUtils.forceMkdir (new File (path));

        entity.setTrainSource (saveFile (trainSource, name));
        entity.setTestSource (saveFile (testSource, name));
        entity.setPredictSource (saveFile (predictSource, name));
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
            return Response.ok ("删除成功");
        } else {
            return Response.fail ("算法不存在");
        }
    }

    @GetMapping(value = "/get")
    @ApiOperation(value = "分页查找")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "size", value = "页大小", required = true),
            @ApiImplicitParam(paramType = "query", name = "num", value = "页号", required = true)
    })
    public Response<?> getAlgoPage(@RequestParam(required = false, defaultValue = "20") Integer size,
                                   @RequestParam(required = false, defaultValue = "1") Integer num) throws IOException {
        return Response.ok (service.query (size, num));
    }

    @GetMapping(value = "/getByName/{name}")
    @ApiOperation(value = "根据name查找")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "name", value = "算法名字", required = true),
    })
    public Response<?> getAlgoPage(@PathVariable String name) throws IOException {
        if (service.exists (name)) {
            return Response.ok (service.getAlgoEntity (name));
        } else {
            return Response.fail ("算法不存在");
        }
    }
}
