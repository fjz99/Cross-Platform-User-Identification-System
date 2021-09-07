package edu.nwpu.cpuis.network;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

class SocketModelClientTest {

    @Test
    void send() throws Exception {
        ModelClient client = new SocketModelClient ("127.0.0.1", 8888);
        client.start ();
        System.out.println (client.send (new Package ("das", OperationType.PAUSE, new HashMap<String, Object> () {
            {
                put ("abc", 123);
            }
        })));
        client.shutdown ();
    }
}

