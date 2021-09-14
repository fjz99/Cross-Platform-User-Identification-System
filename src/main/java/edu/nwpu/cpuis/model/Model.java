package edu.nwpu.cpuis.model;

import java.util.List;

/**
 * @param <A> 模型输入
 * @param <B> 模型输出
 * @author fujiazheng
 */
public interface Model<A, B> {
    boolean train(List<String> files, String name);

    boolean load(String name);

    boolean destroy(String name);

    B predict(A data, String name);
}
