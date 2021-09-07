package edu.nwpu.cpuis.network;

import lombok.Getter;

@Getter
public enum OperationType {
    PAUSE (0x0),
    CONTINUE (0x1),
    LOAD (0x2),
    STOP (0x3),
    TRAIN (0x4),
    PREDICT (0x5),
    GET_TRAINING_PERCENTAGE (0x6),
    NEW_MODEL (0x7);

    private final int code;

    OperationType(int code) {
        this.code = code;
    }
}
