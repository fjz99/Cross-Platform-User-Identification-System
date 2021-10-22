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
        OutputSearchVO vo = OutputSearchVO.builder ()
                .dataset (new String[]{"fb", "fs"})
                .algoName ("hash")
                .phase ("train")
                .build ();
        System.out.println (service.getOutput (vo));
        vo = OutputSearchVO.builder ()
                .dataset (new String[]{"fb", "fs"})
                .algoName ("hash")
                .phase ("train")
                .id (1)
                .build ();
        System.out.println (service.getOutput (vo));
        vo = OutputSearchVO.builder ()
                .dataset (new String[]{"fb", "fs"})
                .algoName ("hash")
                .phase ("train")
                .range (new Integer[]{3, 22})
                .build ();
        System.out.println (service.getOutput (vo));
        vo = OutputSearchVO.builder ()
                .dataset (new String[]{"fb", "fs"})
                .algoName ("hash")
                .phase ("train")
                .k (1)
                .build ();
        System.out.println (service.getOutput (vo));
    }
}
