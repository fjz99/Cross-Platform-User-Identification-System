package edu.nwpu.cpuis.service.model;

/**
 * @author fujiazheng
 * @param <A>
 * @param <B>
 */
public interface InterruptableModel<A, B> extends Model<A, B> {
    boolean stopTrain();

    boolean continue0();

    boolean pause();
}
