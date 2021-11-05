package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.service.AlgoService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RestController()
@RequestMapping("/algo")
@Slf4j
public class AlgoController {
    @Value("${file.algo-base-location}")
    private String algoBaseLocation;
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
                                    @RequestPart(required = false) String contact) throws IOException {
        AlgoEntity entity = AlgoEntity.builder ()
                .author (author)
                .contact (contact)
                .name (name)
                .description (description)
                .build ();
        String path = algoBaseLocation + "/" + name + "/";

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
            String pathname = algoBaseLocation + "/" + name + "/" + file.getOriginalFilename ();
            File file1 = new File (pathname);
            file.transferTo (file1);
            return pathname;
        } else return null;
    }

}
