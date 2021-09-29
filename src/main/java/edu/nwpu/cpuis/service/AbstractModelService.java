package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoEntity;
import edu.nwpu.cpuis.entity.Output;

import javax.annotation.Resource;


public abstract class AbstractModelService {
    @Resource
    protected MongoService<MongoEntity> mongoService;

    public Output getMetadata(String key) {
        Output output = mongoService.selectById (key, MongoEntity.class, "output").getOutput ();
        output.setOutput (null);
        return output;
    }
}
