package edu.nwpu.cpuis.utils;

import edu.nwpu.cpuis.service.model.ModelDefinition;
import edu.nwpu.cpuis.train.SimpleProcessWrapper;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import edu.nwpu.cpuis.train.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PythonUtilsTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ModelDefinition modelDefinition;

    @Test
    void runScript() throws InterruptedException {
        Map<String, Object> args = new HashMap<> ();
        args.put ("dirs", new ArrayList<String> () {{
            add ("abc.txt");
            add ("def.txt");
        }});
        List<String> names = new ArrayList<> ();
        names.add ("dataset1");
        names.add ("dataset2");
        SimpleProcessWrapper demo = PythonScriptRunner.runScript ("demo", "getDaemon-template.py", args, names);
        while (demo.getPercentage () != 100) {
            Thread.sleep (500);
            System.out.println (demo.getPercentage ());
        }
        Thread.sleep (2000); //否则会因为主线程停止导致测试结束，导致错误
        System.out.println (demo.getPercentage ());
//        demo.removeFromMap ();
    }

    @Test
    void test() throws IOException {
        ResourcePatternResolver context = new PathMatchingResourcePatternResolver ();
        String path = context.getResources ("classpath:/**/getDaemon-template.py")[0].getFile ().getPath ();
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
    void testOutput() {
        Map<String, Object> args = new HashMap<> ();
        args.put ("dirs", new ArrayList<String> () {{
            add ("abc.txt");
            add ("def.txt");
        }});
        List<String> names = new ArrayList<> ();
        names.add ("dataset1");
        names.add ("dataset2");
        SimpleProcessWrapper demo = PythonScriptRunner.runScript ("demo", "demo-getDaemon.py", args, names);
        while (demo.getState () != State.SUCCESSFULLY_STOPPED) ;
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

    @Test
    public void runCmdTest() {
        Map<String, Object> args = new HashMap<> ();
        args.put ("dirs", new ArrayList<String> () {{
            add ("E:/inputs/fb");
            add ("E:/inputs/fs");
        }});
        List<String> names = new ArrayList<> ();
        names.add ("fb");
        names.add ("fs");
        args.put ("k", "10");
        SimpleProcessWrapper demo = PythonScriptRunner.runScript ("hash", "MECS-SDA2.py", args, names);
        while (demo.getState () != State.SUCCESSFULLY_STOPPED) ;
        System.out.println (demo.getPercentage ());
        System.out.println (demo.getState ());
        System.out.println (demo.getOutput ());
    }

}
