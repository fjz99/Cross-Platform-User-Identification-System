package edu.nwpu.cpuis.train;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

//hash-fb-fs-train-output
@Slf4j
public abstract class ProcessWrapper {

    protected static final String DONE_LITERAL = "done";
    protected final Process process;
    protected final BufferedReader reader;
    protected final Thread daemon;
    protected volatile State state;//-1 err 0 ok 1 running
    protected String[] dataset;
    protected String phase;
    protected String algoName;
    protected String key;

    public ProcessWrapper(Process process, String algoName, String[] dataset, String phase) {
        this.state = State.TRAINING;
        this.algoName = algoName;
        this.dataset = dataset;
        this.process = process;
        this.phase = phase;
        reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
        daemon = getDaemon ();
        cleanupLastOutput ();
    }

    public void start() {
        PythonScriptRunner.executor.submit (daemon);
    }

    protected abstract void cleanupLastOutput();

    protected abstract Thread getDaemon();

    protected abstract boolean processOutput(String s);

    public void kill() {
        process.destroy ();
    }

    public State getState() {
        return state;
    }

}
