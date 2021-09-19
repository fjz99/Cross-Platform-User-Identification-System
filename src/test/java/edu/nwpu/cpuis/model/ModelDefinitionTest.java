package edu.nwpu.cpuis.model;

import edu.nwpu.cpuis.service.model.ModelDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE
        , properties = {"models.definition.test.scriptSource=123"})
class ModelDefinitionTest {
    @Autowired
    private ModelDefinition definition;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @DisplayName("test")
    @Test
    public void test() {
        System.out.println (definition);
        System.out.println (definition.getByStage (1));
    }

}
