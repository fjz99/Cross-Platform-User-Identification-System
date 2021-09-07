package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.model.ModelDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class Test {
    @Resource
    private ModelDefinition definition;

    @GetMapping("/get")
    public List<ModelDefinition.SingleModel> get() {
        return definition.getByStage (1);
    }

//    @GetMapping("/p")
//    public String gest(@Value("${models.definition.test.name}") String s) {
//        return s;
//    }
}
