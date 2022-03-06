package org.cpuis;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;


public class ProcessTest {
    @Test
    void test() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder ();
        URL resource = getClass ().getClassLoader ().getResource ("123.py");
        System.out.println (resource.getPath ());
        Process exec = processBuilder
                .command ("python",resource.getPath ().substring (1))
//                .redirectOutput (new File ("11.txt"))
//                .redirectError (new File ("22.txt"))
                .start ();
//        Process exec = Runtime.getRuntime ().exec ("python -c 'print(1)'");
        InputStream errorStream = exec.getErrorStream ();
        BufferedReader reader = new BufferedReader (new InputStreamReader (errorStream));
        String c;
        while ((c = reader.readLine ()) != null) {
            System.out.println (c);
        }
    }
}
