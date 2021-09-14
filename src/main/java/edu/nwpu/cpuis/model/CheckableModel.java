package edu.nwpu.cpuis.model;

/**
 * @param <A>
 * @param <B>
 * @author fujiazheng
 */
public interface CheckableModel<A, B> extends Model<A, B>{
    double getPercentage(String name);
}
