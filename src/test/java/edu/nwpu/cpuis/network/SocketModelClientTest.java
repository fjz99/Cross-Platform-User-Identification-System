package edu.nwpu.cpuis.network;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

@SpringBootTest
class SocketModelClientTest {

    @Test
    void send() throws Exception {
        ModelClient client = new SocketModelClient ();
        System.out.println (client.send (new Package (-1, "das", OperationType.PAUSE, new HashMap<String, Object> () {
            {
                put ("abc", 123);
            }
        })));
    }
}

