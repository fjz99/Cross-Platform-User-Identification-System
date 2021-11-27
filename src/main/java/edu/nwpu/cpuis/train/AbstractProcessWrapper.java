package edu.nwpu.cpuis.train;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.Output;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//hash-fb-fs-train-output
@Slf4j
public abstract class AbstractProcessWrapper {

    protected static final String DONE_LITERAL = "done";
    protected static final long waitProcessTerminationTimeout = 1000;//ms

    protected final Process process;
    protected final BufferedReader reader;

    protected volatile State state;
    protected volatile double percentage = 0;
    protected volatile boolean parseOutput = false;

    protected String phase;
    protected String algoName;
    protected String key;
    protected String[] dataset;
    protected Future<?> future;
    protected Output output;
    protected String errMsg;

    public AbstractProcessWrapper(Process process, String algoName, String[] dataset, String phase) {
        this.state = State.TRAINING;
        this.algoName = algoName;
        this.dataset = dataset;
        this.process = process;
        this.phase = phase;
        reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
        cleanupLastOutput ();
    }

    /**
     * 这个方法应该完全地释放资源
     * 资源有2个：监视线程，运行计算的进程
     */
    public final void stop() {
        boolean canceled = false;
        try {
            process.destroyForcibly ();
            process.waitFor (waitProcessTerminationTimeout, TimeUnit.MILLISECONDS);
            if (!process.isAlive ()) {
                canceled = true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }

        if (!canceled) {
            log.error ("{} can not be canceled.", key);
            log.error ("canceled timeout({}ms)", waitProcessTerminationTimeout);
            throw new CpuisException (ErrCode.MODEL_CANNOT_CANCELED,
                    String.format ("canceled timeout(%sms)", waitProcessTerminationTimeout));
        }
        removeFromMap ();//只有取消成功了才删除map，否则可以一直取消,但是线程canceled失败就不管了
        if (!future.cancel (true)) {
            if (!(future.isDone () || future.isCancelled ())) {
                log.error ("{} can not be canceled (thread canceled failed).", key);
                throw new CpuisException (ErrCode.MODEL_CANNOT_CANCELED, "thread canceled failed");
            }
        }
        log.info ("{} is canceled successfully.", key);
    }

    public void start() {
        future = PythonScriptRunner.executor.submit (() -> {
            try {
                readFromScript ();
            } catch (IOException e) {
                e.printStackTrace ();
                state = State.ERROR_STOPPED;
                removeFromMap ();
                log.error ("{} err stop process :{}", key, e.getMessage ());
            }
        });

        PythonScriptRunner.executor.submit (this::readFromErrStream);
    }

    protected void readFromErrStream() {
        StringBuilder sb = new StringBuilder ();
        Arrays.sort (dataset);
        key = String.format ("%s-%s-%s-%s", algoName, dataset[0], dataset[1], phase);
        String s;
        try {
            while ((s = reader.readLine ()) != null) {
                sb.append (s);
            }
            if (sb.length () > 0) {
                this.errMsg = sb.toString ();
                this.state = State.ERROR_STOPPED;
                log.error ("{} 获得标准错误流输出 {}", key, errMsg);
            }
        } catch (IOException e) {
            e.printStackTrace ();
        }
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
        } catch (Exception e) {
            e.printStackTrace ();
            log.error ("output parse err " + e.getMessage ());
            return false;
        }
    }

    protected void readFromScript() throws IOException {
        StringBuilder sb = new StringBuilder ();
        Arrays.sort (dataset);
        key = String.format ("%s-%s-%s-%s", algoName, dataset[0], dataset[1], phase);
        String s;
        while (state == State.TRAINING) {
            //没有数据读会阻塞，如果返回null，就是进程结束了
            if ((s = reader.readLine ()) == null) {
                if (parseOutput && processOutput (sb.toString ().trim ())) {
                    state = State.SUCCESSFULLY_STOPPED;
                    afterScriptDone ();
                    log.info ("{} successfully stopped", key);
                } else {
                    int exitValue = process.exitValue ();
                    log.error ("{} err shutdown stream with exit value {}", key, exitValue);
                    state = State.ERROR_STOPPED;
                }
                break;
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
                        log.warn ("{} err max percentage is: {}", key, percentage);
                        percentage = 100;
                    } else {
                        log.debug ("{} changed to get output", key);
                    }
                } else {
                    state = State.ERROR_STOPPED;
                    log.error ("{} err input: {}", key, s);
                }
            }
        }
    }

}
