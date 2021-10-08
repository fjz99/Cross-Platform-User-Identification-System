package edu.nwpu.cpuis.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MatrixOutputModelServiceTest {

    @Autowired
    private MatrixOutputModelService service;

    @Test
    void getMatchedUsers() {
        System.out.println (service.getMatchedUsers ("2", "demo-a-b-train", true));
        System.out.println (service.getMatchedUsers ("1", "demo-a-b-train", true));
        System.out.println (service.getMatchedUsers ("1", "demo-a-b-train", false));
        System.out.println (service.getMatchedUsers ("2", "demo-a-b-train", false));
    }
}
