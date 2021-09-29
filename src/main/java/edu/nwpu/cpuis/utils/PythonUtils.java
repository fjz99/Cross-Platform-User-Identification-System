package edu.nwpu.cpuis.utils;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.MongoEntity;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.service.model.BasicModel;
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
import java.util.*;

/**
 * @author fujiazheng
 */
@Component
@Slf4j
public final class PythonUtils implements ApplicationContextAware {
    //key
    private static final Map<String, ProcessWrapperTrain> processes = new HashMap<> ();
    private static final String TRAIN_TYPE_NAME = "train";
    private static final String PREDICT_TYPE_NAME = "predict";
    static MongoService<MongoEntity> mongoService;
    private static ApplicationContext context;
    private static ThreadPoolTaskExecutor executor;


    private PythonUtils(ThreadPoolTaskExecutor executor, MongoService<MongoEntity> mongoService) {
        PythonUtils.mongoService = mongoService;
        PythonUtils.executor = executor;
    }

    /**
     * @param algoName
     * @param sourceName
     * @param args   存在dirs=[E:/hou,xx]，是数据集的位置
     * @param datasetNames
     * @return
     */
    public static ProcessWrapperTrain runScript(String algoName, String sourceName, Map<String, Object> args, List<String> datasetNames) {
        try {
            String cmd = buildCmd (args, sourceName);
            log.info ("run script cmd '{}'", cmd);
            Process exec = Runtime.getRuntime ().exec (cmd);
            String key = generateMongoKey (algoName,
                    datasetNames,
                    TRAIN_TYPE_NAME);
            ProcessWrapperTrain wrapper = new ProcessWrapperTrain (exec, key);
            processes.put (key, wrapper);
            return wrapper;
        } catch (IOException e) {
            e.printStackTrace ();
            return null;
        }
    }


    private static String buildCmd(Map<String, Object> args, String sourceName) throws IOException {
        String path = context.getResources ("classpath:/**/" + sourceName)[0].getFile ().getPath ();
        String cmd = String.format ("python %s", path);
        StringBuilder sb = new StringBuilder (cmd);
        args.forEach ((k, v) -> sb.append (" --").append (k).append ('=').append (v));
        return sb.toString ();
    }

    public static ProcessWrapperTrain getTrainProcess(String key) {
        return processes.getOrDefault (key, null);
    }

    private static String generateMongoKey(String algoName, List<String> datasetName, String type) {
        Collections.sort (datasetName);
        return String.format ("%s-%s-%s-%s", algoName, datasetName.get (0), datasetName.get (1), type);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static enum State {
        UNTRAINED,
        TRAINING,
        ERROR_STOPPED,//任何异常
        SUCCESS_STOPPED,
        PREDICTING;
    }

    @Slf4j
    public abstract static class ProcessWrapper {

        protected static final String DONE_LITERAL = "done";
        protected final Process process;
        protected final BufferedReader reader;
        protected final Thread daemon;
        protected final String key;
        protected volatile PythonUtils.State state;//-1 err 0 ok 1 running

        public ProcessWrapper(Process process, String key) {
            this.state = PythonUtils.State.TRAINING;
            this.key = key;
            this.process = process;
            reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
            daemon = getDaemon ();
            executor.submit (daemon);
        }

        abstract protected Thread getDaemon();

        public void kill() {
            process.destroy ();
        }

        public PythonUtils.State getState() {
            return state;
        }
    }

    public static class ProcessWrapperTrain extends ProcessWrapper {
        private volatile double percentage;
        private volatile boolean parseOutput;
        private Output output;

        public ProcessWrapperTrain(Process process, String name) {
            super (process, name);
            percentage = 0;
            parseOutput = false;
        }

        public Output getOutput() {
            if (state == PythonUtils.State.SUCCESS_STOPPED)
                return output;
            else return null;
        }

        public double getPercentage() {
            return percentage;
        }

        @Override
        protected Thread getDaemon() {
            return new Thread (() -> {
                String s;
                StringBuilder sb = new StringBuilder ();
                try {
                    while (state == PythonUtils.State.TRAINING) {
                        //没有数据读会阻塞，如果返回null，就是进程结束了
                        if ((s = reader.readLine ()) == null) {
                            if (parseOutput && precessOutput (sb.toString ())) {
                                saveToMongoDB (key, output);
                                state = PythonUtils.State.SUCCESS_STOPPED;
                                log.info ("{} successfully stopped", key);
                            } else {
                                log.error ("{} err shutdown stream", key);
                                state = PythonUtils.State.ERROR_STOPPED;
                            }
                        }
                        if (parseOutput) {
                            //处理JSON
                            sb.append (s.trim ());
                        } else if (NumberUtils.isCreatable (s)) {
                            percentage = Double.parseDouble (s);
                            log.debug ("{} percentage changed: {}", key, percentage);
                        } else {
                            //不是小数，规定结束符为DONE_LITERAL
                            if (StringUtils.equals (s, DONE_LITERAL)) {
                                parseOutput = true;
                                if (percentage != 100) {
//                                    shutDownReason = -1;
                                    log.warn ("{} err max percentage is: {}", key, percentage);
                                    percentage = 100;
                                } else {
//                                    shutDownReason = 0;
                                    log.debug ("{} changed to get output", key);
                                }
                            } else {
                                state = PythonUtils.State.ERROR_STOPPED;
                                log.error ("{} err input: {}", key, s);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace ();
                    state = PythonUtils.State.ERROR_STOPPED;
                    PythonUtils.processes.remove (key);
                    log.error ("{} err stop process :{}", key, e.getMessage ());
                }
            });
        }

        private void saveToMongoDB(String key, Output output) {
            MongoEntity mongoEntity = new MongoEntity ();
            mongoEntity.setOutput (output);
            mongoEntity.set_id (key);
            log.info ("{} saved output to mongodb", key);
            mongoService.insert (mongoEntity, "output");
        }

        private boolean precessOutput(String s) {
            try {
                output = JSON.parseObject (s, Output.class);
                return true;
            } catch (Exception e) {
                e.printStackTrace ();
                log.error ("output parse err " + e.getMessage ());
                return false;
            }
        }

        @Override
        public void kill() {
            super.kill ();
            processes.remove (key);
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
