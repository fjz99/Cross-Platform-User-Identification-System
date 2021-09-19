package edu.nwpu.cpuis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileInputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ModelControllerTest {

    @Autowired
    private MockMvc mvc;

    //还需要postman，mock无法模拟tempdir
    @Test
    void uploadInputs() throws Exception {
        MockMultipartFile firstFile =
                new MockMultipartFile ("file", "E:/123.zip", MediaType.TEXT_PLAIN_VALUE, new FileInputStream ("E:/123.zip"));
        MockPart part = new MockPart ("name", "fuckyou".getBytes ());
        mvc.perform (multipart ("/model/uploadInputs").file (firstFile).part (part))
                .andExpect (status ().isOk ());
    }
}
