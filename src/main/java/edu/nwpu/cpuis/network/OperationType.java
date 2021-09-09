package edu.nwpu.cpuis.network;

import lombok.Getter;

/**
 * @author fujiazheng
 */
@Getter
public enum OperationType {
    PAUSE (0x0),
    CONTINUE (0x1),
    LOAD (0x2),
    STOP (0x3),//停止训练
    TRAIN (0x4),
    PREDICT (0x5),
    GET_TRAINING_PERCENTAGE (0x6),
    NEW_MODEL (0x7),//提供model名，python源文件地址等信息
    DESTROY (0x8);

    private final int code;

    OperationType(int code) {
        this.code = code;
    }
}
