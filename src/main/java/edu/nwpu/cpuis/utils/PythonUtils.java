package edu.nwpu.cpuis.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fujiazheng
 */
@Component
@Slf4j
public final class PythonUtils implements ApplicationContextAware {
    private static final Map<String, ProcessWrapperTrain> trainProcesses = new HashMap<> ();
    private static final Map<String, ProcessWrapperPredict> predictProcesses = new HashMap<> ();//?
    private static ApplicationContext context;
    private static ThreadPoolTaskExecutor executor;


    private PythonUtils(ThreadPoolTaskExecutor executor) {
        PythonUtils.executor = executor;
    }

    public static ProcessWrapperTrain runScript(String algoName, String sourceName, Map<String, String> args) {
        try {
            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            Process exec = Runtime.getRuntime ().exec (cmd);
            ProcessWrapperTrain wrapper = new ProcessWrapperTrain (exec, algoName);
            trainProcesses.put (algoName, wrapper);
            return wrapper;
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }
    }

    private static String buildCmd(Map<String, String> args, String sourceName) throws IOException {
        String path = context.getResources ("classpath:/**/" + sourceName)[0].getFile ().getPath ();
        String cmd = String.format ("python %s", path);
        StringBuilder sb = new StringBuilder (cmd);
        args.forEach ((k, v) -> sb.append (" --").append (k).append ('=').append (v));
        return sb.toString ();
    }

    public static ProcessWrapperTrain getTrainProcess(String name) {
        return trainProcesses.getOrDefault (name, null);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Slf4j
    public abstract static class ProcessWrapper {
        protected static final String DONE_LITERAL = "done";
        protected final Process process;
        protected final BufferedReader reader;
        protected final Thread daemon;
        protected final String name;
        protected volatile int shutDownReason;//-1 err 0 ok 1 running

        public ProcessWrapper(Process process, String name) {
            this.shutDownReason = 1;
            this.name = name;
            this.process = process;
            reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
            daemon = getDaemon ();
            executor.submit (daemon);
        }

        abstract protected Thread getDaemon();

        public void kill() {
            process.destroy ();
        }
    }

    public static class ProcessWrapperTrain extends ProcessWrapper {
        private volatile double percentage;

        public ProcessWrapperTrain(Process process, String name) {
            super (process, name);
            percentage = 0;
        }

        public double getPercentage() {
            return percentage;
        }

        @Override
        protected Thread getDaemon() {
            return new Thread (() -> {
                String s;
                try {
                    while (shutDownReason == 1) {
                        //没有数据读会阻塞，如果返回null，就是进程结束了
                        if ((s = reader.readLine ()) == null) {
                            log.error ("err shutdown stream");
                            shutDownReason = -1;
                        } else if (NumberUtils.isDigits (s)) {
                            percentage = Double.parseDouble (s);
                            log.debug ("{} percentage changed: {}", name, percentage);
                        } else {
                            //不是小数，规定结束符为DONE_LITERAL
                            if (StringUtils.equals (s, DONE_LITERAL)) {
                                if (percentage != 100) {
                                    shutDownReason = -1;
                                    log.error ("err max percentage is: {}", percentage);
                                } else {
                                    shutDownReason = 0;
                                    log.info ("train model {} success", name);
                                }
                            } else {
                                shutDownReason = -1;
                                log.error ("err input: {}", s);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace ();
                    shutDownReason = -1;
                    PythonUtils.trainProcesses.remove (name);
                    log.error ("err stop process :" + e.getMessage ());
                }
            });
        }

        @Override
        public void kill() {
            super.kill ();
            trainProcesses.remove (name);
        }
    }

    //TODO
    public static class ProcessWrapperPredict extends ProcessWrapper {

        public ProcessWrapperPredict(Process process, String name) {
            super (process, name);
        }

        @Override
        protected Thread getDaemon() {
            return null;
        }
    }
}
