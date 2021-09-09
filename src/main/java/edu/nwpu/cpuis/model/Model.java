package edu.nwpu.cpuis.model;

import java.util.List;

/**
 * @param <A> 模型输入
 * @param <B> 模型输出
 * @author fujiazheng
 */
public interface Model<A, B> {
    void train(List<String> files, String name);

    boolean load(String name);

    boolean destroy();

    B predict(A data);
}
