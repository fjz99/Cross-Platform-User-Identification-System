package edu.nwpu.cpuis.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.nwpu.cpuis.utils.TypeSerializer;

//还有一种分类方式，
//    https://blog.csdn.net/qq_40610003/article/details/116587172
//    借助于properties文件
@JsonSerialize(using = TypeSerializer.class)
public enum ErrCode {
    SUCCESS (0x0),
    UNKNOWN_ERR (0xffff_ffff), //服务器发生异常，如空指针异常等
    GENERIC_ERR (0xefff_ffff), //普通错误
    //model
    MODEL_NOT_EXISTS (0x1001),
    MODEL_ALREADY_STOPPED (0x1002),
    MODEL_IN_TRAINING (0x1003),
    MODEL_CANNOT_CANCELED (0x1004),//无法终止任务
    //dataset
    DATASET_VALIDATION_FAILED (0x2001),
    DATASET_NOT_EXISTS (0x2002),
    //algo
    ALGO_VALIDATION_FAILED (0x3001),
    ALGO_NOT_EXISTS (0x3002),
    //interact
    WRONG_DATASET_INPUT (0x4001),
    WRONG_STAGE_INPUT(0x4002);

    private final int code;
    private final String msg;

    ErrCode(int code) {
        this.code = code;
        this.msg = this.name ();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
