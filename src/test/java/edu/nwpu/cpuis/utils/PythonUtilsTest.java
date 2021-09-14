package edu.nwpu.cpuis.utils;

import edu.nwpu.cpuis.model.ModelDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.validation.annotation.Validated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PythonUtilsTest {

    @Value("classpath*:/**/demo-train.py")
    private Resource[] resource;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ModelDefinition modelDefinition;

    @Test
    void runScript() throws InterruptedException {
        Map<String, String> args = new HashMap<> ();
        args.put ("file", "123.txt");
        PythonUtils.ProcessWrapperTrain demo = PythonUtils.runScript ("demo", "demo-train.py", args);
        while (demo.getPercentage () != 100) {
            System.out.println (demo.getPercentage ());
        }
        System.out.println (demo.getPercentage ());
//        demo.kill ();
    }

    @Test
    void test() throws IOException {
        ResourcePatternResolver context = new PathMatchingResourcePatternResolver ();
        context.getResources ("classpath:/**/demo-train.py");
        String path = resource[0].getFile ().getPath ();
        System.out.println (path);
        String x = "python " + path + " --file=123.txt";
        System.out.println (x);
        Process process = Runtime.getRuntime ().exec (x);
        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
        while (true) {
            String s = reader.readLine ();
            if (s == null) break;
            System.out.println (s);
        }
    }

    @Test
    public void getPath() throws IOException {
        System.out.println (applicationContext.getResource ("classpath*:/python/model/*.py"));
        System.out.println (Arrays.toString (applicationContext.getResources ("classpath*:/python/model/*.py")));
    }

    @Test
    public void test1() throws IOException {
        System.out.println (modelDefinition);
    }

}
