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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PythonUtilsTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ModelDefinition modelDefinition;

    @Test
    void runScript() {
        Map<String, Object> args = new HashMap<> ();
        args.put ("dirs", new ArrayList<String> () {{
            add ("abc.txt");
            add ("def.txt");
        }});
        PythonUtils.ProcessWrapperTrain demo = PythonUtils.runScript ("demo", "train-template.py", args);
        while (demo.getPercentage () != 100) {
            System.out.println (demo.getPercentage ());
        }
        System.out.println (demo.getPercentage ());
//        demo.kill ();
    }

    @Test
    void test() throws IOException {
        ResourcePatternResolver context = new PathMatchingResourcePatternResolver ();
        String path=context.getResources ("classpath:/**/train-template.py")[0].getFile ().getPath ();
//        String path = resource[0].getFile ().getPath ();
        System.out.println (path);
        String x = "python " + path + " --dirs=[123.txt,456.txt]";
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
    void testOutput(){
        Map<String, Object> args = new HashMap<> ();
        args.put ("dirs", new ArrayList<String> () {{
            add ("abc.txt");
            add ("def.txt");
        }});
        PythonUtils.ProcessWrapperTrain demo = PythonUtils.runScript ("demo", "demo-train.py", args);
        while (demo.getState () != PythonUtils.ProcessWrapper.State.SUCCESS_TOP) ;
        System.out.println (demo.getPercentage ());
        System.out.println (demo.getState ());
        System.out.println (demo.getOutput ());
    }

    @Test
    public void getPath() throws IOException {
        System.out.println (applicationContext.getResource ("classpath*:/python/model/*.py"));
        System.out.println (Arrays.toString (applicationContext.getResources ("classpath*:/python/model/*.py")));
    }

    @Test
    public void test1() {
        System.out.println (modelDefinition);
    }

}
