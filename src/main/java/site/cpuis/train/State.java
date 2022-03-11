package site.cpuis.train;


public enum State {
    UNTRAINED,
    TRAINING,
    ERROR_STOPPED,//任何异常
    SUCCESSFULLY_STOPPED,
    PREDICTING,
    INTERRUPTED, //被终止
    PENDING,//等待执行
    PROCESSING_OUTPUT;
}
