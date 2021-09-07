package edu.nwpu.cpuis.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE
        ,properties = {"models.definition.test.scriptSource=123"})
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

        System.setProperty ("models.definition.test.scriptSource", "123");
        applicationContext.stop ();
        applicationContext.start ();
        System.out.println (applicationContext.getBean (ModelDefinition.class));
    }

}
