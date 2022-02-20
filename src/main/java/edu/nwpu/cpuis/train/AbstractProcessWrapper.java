package edu.nwpu.cpuis.train;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.*;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import edu.nwpu.cpuis.service.MongoService;
import edu.nwpu.cpuis.utils.ModelKeyGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//hash-fb-fs-train-output

/**
 * 训练状态监控功能
 * 只有训练结束时才会写入数据库，其他情况下都是保存在内存中，这样速度快
 * 单独开一个守护线程，每隔一段时间去检查一次状态，并更新状态，注意volatile即可
 * 当线程结束时，需要kill守护线程
 * 线程池相当于一个队列，所以初始状态是PENDING
 * 使用{@link ModelTrainingInfoService}对接Spring Cache,这样可以方便切换redis、memcached
 * 先写缓存，最后才写数据库
 * todo 更新太频繁
 */
@Slf4j
@Getter
public abstract class AbstractProcessWrapper {

    public static final String MONGO_NAME = PythonScriptRunner.modelTrainingInfoMongoName;
    public static final int THREAD_CHECK_INTERVAL = 500;
    protected static final String DONE_LITERAL = "done";
    protected static final long waitProcessTerminationTimeout = 1000;//ms
    protected static MongoService<ModelTrainingInfo> mongoService = PythonScriptRunner.modelTrainingInfoMongoService;
    protected final Process process;
    protected final BufferedReader inputStreamReader;
    protected final BufferedReader errStreamReader;
    protected final String id;
    protected volatile State state;
    protected volatile String reason;
    protected volatile double percentage = 0;
    protected volatile boolean parseOutput = false;
    protected String phase;
    protected String algoName;
    protected String key;
    protected String[] dataset;
    protected Future<?> future;
    protected Output output;
    protected AlgoEntity algoEntity;
    protected List<DatasetManageEntity> datasetEntityList;
    protected long startTime;
    protected ModelTrainingInfo modelTrainingInfo;
    protected ModelTrainingInfoService modelTrainingInfoService = PythonScriptRunner.modelTrainingInfoService;
    private volatile boolean daemonStopFlag = false;
    private Future<?> daemon;

    public AbstractProcessWrapper(Process process, String algoName, String[] dataset, String phase) {
        this.state = State.PENDING;
        this.algoName = algoName;
        this.algoEntity = PythonScriptRunner.algoService.getAlgoEntity (algoName);
        this.dataset = dataset;
        this.id = ModelKeyGenerator.generateKey (dataset, algoName, phase, null);
        this.datasetEntityList = Arrays.stream (dataset).map (
                name -> PythonScriptRunner.datasetService.getEntity (name)
        ).collect (Collectors.toList ());
        this.process = process;
        this.phase = phase;
        this.inputStreamReader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
        this.errStreamReader = new BufferedReader (new InputStreamReader (process.getErrorStream ()));

        setModelTrainingInfo ();
        cleanupLastOutput ();
    }


    public ModelTrainingInfo getModelTrainingInfo() {
        return modelTrainingInfo;
    }

    /**
     * 这个方法应该完全地释放资源
     * 资源有2个：监视线程，运行计算的进程
     */
    public final void stop() {
        if (state != State.TRAINING && state != State.PREDICTING) {
            log.error ("{} stop失败，因为当前状态为 {}", key, state);
            failed (key + " stop失败，因为当前状态为 " + state);
            throw new CpuisException (ErrCode.MODEL_CANNOT_CANCELED,
                    String.format ("%s无法stop，因为模型当前状态为%s，不在训练中", key, state));
        }
        boolean canceled = false;
        try {
            process.destroyForcibly ();
            process.waitFor (waitProcessTerminationTimeout, TimeUnit.MILLISECONDS);
            if (!process.isAlive ()) {
                canceled = true;
            }
        } catch (InterruptedException e) {
            log.error ("", e);
        }

        if (!canceled) {
            log.error ("{} can not be canceled.", key);
            log.error ("canceled timeout({}ms)", waitProcessTerminationTimeout);
            failed ("stop失败，canceled timeout  " + waitProcessTerminationTimeout + " ms");
            throw new CpuisException (ErrCode.MODEL_CANNOT_CANCELED,
                    String.format ("canceled timeout(%sms)", waitProcessTerminationTimeout));
        }
        removeFromMap ();//只有取消成功了才删除map，否则可以一直取消,但是线程canceled失败就不管了
        //下面关闭监控线程
        if (!future.cancel (true)) {
            if (!(future.isDone () || future.isCancelled ())) {
                log.error ("{} can not be canceled (thread canceled failed).", key);
                failed ("stop失败，thread canceled failed");
                throw new CpuisException (ErrCode.MODEL_CANNOT_CANCELED, "thread canceled failed");
            }
        }
        cleanupLastOutput ();//
        interrupt ("成功终止训练");
        finishModel ();
        log.info ("{} is canceled successfully.", key);
    }

    public void start() {
        future = PythonScriptRunner.executor.submit (() -> {
            beforeStart ();

            try {
                readFromScript ();
            } catch (IOException e) {
                log.error ("", e);
                failed ("进程启动失败 " + e.getMessage ());
                removeFromMap ();
                log.error ("{} start failed :{}", key, e.getMessage ());
            }
            log.debug ("readFromScript stopped state={}", state);
        });

        PythonScriptRunner.executor.submit (this::readFromErrStream);
    }

    protected void beforeStart() {
        state = phase.equals ("train") ? State.TRAINING : State.PREDICTING;
        startTime = System.currentTimeMillis ();
        modelTrainingInfo.setState (state);
        modelTrainingInfoService.setCache (modelTrainingInfo);
        daemon = PythonScriptRunner.executor.submit (new Daemon ());
    }

    private void setModelTrainingInfo() {
        modelTrainingInfo = ModelTrainingInfo.builder ()
                .id (id)
                .algo (algoEntity)
                .dataset (datasetEntityList)
                .state (state)
                .submitTime (LocalDateTime.now ())
                .build ();
        modelTrainingInfoService.setCache (modelTrainingInfo);
        log.info ("cache key {}", id);
    }

    protected void readFromErrStream() {
        StringBuilder sb = new StringBuilder ();
        Arrays.sort (dataset);
        key = getKey ();
        String s;
        try {
            while ((s = errStreamReader.readLine ()) != null) {
                sb.append (s).append ('\n');
            }
            if (sb.length () > 0) {
                String errMsg = sb.toString ();
                modelTrainingInfo.setErrStreamOutput ("获得标准错误流输出:\n" + errMsg);
                modelTrainingInfoService.setCache (modelTrainingInfo);
                log.error ("{} 获得标准错误流输出 {}", key, errMsg);
            }
        } catch (IOException e) {
            e.printStackTrace ();
        } finally {
            log.debug ("finish model:{}", sb);
            finishModel ();
        }
    }

    protected void success(String msg) {
        setAllState (State.SUCCESSFULLY_STOPPED, msg);
    }

    protected void failed(String msg) {
        setAllState (State.ERROR_STOPPED, msg);
    }

    protected void interrupt(String msg) {
        setAllState (State.INTERRUPTED, msg);
    }

    protected void setAllState(State newState, String msg) {
        log.debug (" {} set state {} -> {}", id, state, newState);
        state = newState;
        modelTrainingInfo.setMessage (msg);
        modelTrainingInfo.setState (newState);
        updateModelTrainingInfo ();
    }

    protected void finishModel() {
        //表示结束，所以插入数据库
        modelTrainingInfo.setTrainingTime (prettyTime (System.currentTimeMillis () - startTime));
        modelTrainingInfoService.setCacheAndMongo (modelTrainingInfo);
        modelTrainingInfo.setState (state);
        killDaemon ();
    }

    protected void killDaemon() {
        daemonStopFlag = true;
        daemon.cancel (true);
    }

    protected abstract void cleanupLastOutput();

    protected abstract void removeFromMap();

    protected abstract void afterScriptDone();

    public final State getState() {
        return state;
    }

    protected boolean processOutput(String s) {
        try {
            output = JSON.parseObject (s, Output.class);
            return true;
        } catch (Throwable e) {
            failed ("python脚本输出格式错误: " + e.getMessage ());
            log.error ("output parse err " + e.getMessage (), e);
            return false;
        }
    }

    private String getKey() {
        return id;
    }

    protected final void readFromScript() throws IOException {
        StringBuilder sb = new StringBuilder ();
        Arrays.sort (dataset);
        key = getKey ();
        String s;
        while (state == State.TRAINING) {
            //没有数据读会阻塞，如果返回null，就是进程结束了
            if ((s = inputStreamReader.readLine ()) == null) {
                int exitValue = process.exitValue ();
                if (exitValue != 0) {
                    reason = String.format ("%s python进程返回值为 %s != 0", key, exitValue);
                    failed (reason);
                    log.error (reason);
                } else {
                    String output = sb.toString ().trim ();
                    if (parseOutput) {
                        if (output.length () == 0) {
                            failed ("python脚本输出为空");
                            log.error ("{} python脚本输出为空", key);
                        } else if (processOutput (output)) {
                            success ("模型正常终止");
                            afterScriptDone ();
                            log.info ("{} successfully stopped", key);
                        }
                    }
                }
                break;
            }

            if (parseOutput) {
                //处理JSON
                sb.append (s.trim ());
            } else if (NumberUtils.isCreatable (s)) {
                percentage = Double.parseDouble (s);
                updateModelTrainingInfo ();
                log.debug ("{} percentage changed: {}", key, percentage);
            } else {
                //不是小数，规定结束符为DONE_LITERAL
                if (StringUtils.equals (s, DONE_LITERAL)) {
                    parseOutput = true;
                    if (percentage != 100) {
                        log.warn ("{} err max percentage is: {}", key, percentage);
                        percentage = 100;
                    } else {
                        log.debug ("{} changed to get output", key);
                    }
                } else {
                    failed ("未知错误: " + s);
                    log.error ("{} err input: {}", key, s);
                }
            }
        }
    }


    protected String prettyTime(long time) {
        if (time < 1000) return time + " ms";
        double p = time;
        p = p / 1000;
        if (p < 60) return String.format ("%.2f s", p);
        p = p / 60;
        if (p < 60) return String.format ("%.2f min", p);
        p = p / 60;
        return String.format ("%.2f h", p);
    }

    protected void updateModelTrainingInfo() {
        modelTrainingInfo.setState (state);
        modelTrainingInfo.setTrainingTime (prettyTime (System.currentTimeMillis () - startTime));
        modelTrainingInfo.setPercentage (percentage);
        modelTrainingInfoService.setCache (modelTrainingInfo);
    }

    private class Daemon extends Thread {
        @Override
        public void run() {
            try {
                while (!daemonStopFlag) {
                    updateModelTrainingInfo ();
                    Thread.sleep (THREAD_CHECK_INTERVAL);
                }
                log.info ("{} Daemon stop", modelTrainingInfo.getId ());
            } catch (InterruptedException e) {
                if (!daemonStopFlag) {
                    log.error ("", e);
                    failed ("?????");
                }
            }
        }
    }

}
