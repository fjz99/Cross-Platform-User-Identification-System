package edu.nwpu.cpuis.train;


public enum State {
    UNTRAINED,
    TRAINING,
    ERROR_STOPPED,//任何异常
    SUCCESSFULLY_STOPPED,
    PREDICTING,
    INTERRUPTED //被终止
}
