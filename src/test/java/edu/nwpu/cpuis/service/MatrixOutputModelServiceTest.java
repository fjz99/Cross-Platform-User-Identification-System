package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MatrixOutputModelServiceTest {

    @Autowired
    private MatrixOutputModelService service;

    @Test
    void getOutput() {
    }
}
