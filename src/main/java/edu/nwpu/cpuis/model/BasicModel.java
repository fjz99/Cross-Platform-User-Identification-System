package edu.nwpu.cpuis.model;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @date 2021/9/14 14:31
 * @author fujiazheng
 */
@Service
public class BasicModel<A, B> implements Model<A, B> {
    @Override
    public void train(List<String> files, String name) {

    }

    @Override
    public boolean load(String name) {
        return false;
    }

    @Override
    public boolean destroy() {
        return false;
    }

    @Override
    public B predict(A data) {
        return null;
    }
}
