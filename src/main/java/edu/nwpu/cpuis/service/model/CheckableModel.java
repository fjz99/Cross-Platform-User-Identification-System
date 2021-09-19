package edu.nwpu.cpuis.service.model;

/**
 * @param <A>
 * @param <B>
 * @author fujiazheng
 */
public interface CheckableModel<A, B> extends Model<A, B>{
    Double getPercentage(String name);
}
