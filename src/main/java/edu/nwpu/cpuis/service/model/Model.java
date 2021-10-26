package edu.nwpu.cpuis.service.model;

import java.util.List;
import java.util.Map;

/**
 * @param <A> 模型输入
 * @param <B> 模型输出
 * @author fujiazheng
 */
public interface Model<A, B> {
    boolean train(List<String> files, String name, Map<String, String> args);

    boolean load(String name);

    boolean destroy(String name);

    B predict(A data, String name);
}
