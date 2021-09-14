package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.model.ModelDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author fujiazheng
 * @date 2021/9/14 14:33
 */
@RestController("/model")
public class ModelController {
    @Resource
    private ModelDefinition definition;

    @GetMapping("{name}/info")
    public ResponseEntity<?> getInfo(@PathVariable("name") String name) {
        if (definition.getDefinition ().containsKey ("name")) {
            return ResponseEntity.ok (definition.getDefinition ().get ("name"));
        } else {
            return ResponseEntity.noContent ().build ();
        }
    }
}
