package edu.nwpu.cpuis.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.nwpu.cpuis.utils.TypeSerializer;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public final class Response<T> {
    private final long timeStamp;
    private final T data;
    private final boolean success;
    private final ErrCode err;

    private Response(T data, boolean success, ErrCode errCode) {
        this.err = errCode;
        timeStamp = System.currentTimeMillis ();
        this.data = data;
        this.success = success;
    }

    private Response(T data, boolean success) {
        this.err = ErrCode.SUCCESS;
        timeStamp = System.currentTimeMillis ();
        this.data = data;
        this.success = success;
    }

    public static <Q> Response<Q> ok(Q a) {
        return new Response<> (a, true);
    }

    public static Response<?> ok() {
        return new Response<> ("ok", true);
    }

    public static <Q> Response<Q> ofOk(Q a, ErrCode errCode) {
        return new Response<> (a, true, errCode);
    }

    public static Response<?> ofOk(ErrCode errCode) {
        return new Response<> ("", true, errCode);
    }

    public static <Q> Response<Q> fail(Q a) {
        return new Response<> (a, false, ErrCode.GENERIC_ERR);
    }

    public static <Q> Response<Q> ofFailed(Q a, ErrCode errCode) {
        return new Response<> (a, false, errCode);
    }

    public static Response<?> ofFailed(ErrCode errCode) {
        return new Response<> ("", false, errCode);
    }

    public static <Q> Response<Q> of(Q a, boolean success, ErrCode errCode) {
        return new Response<> (a, success, errCode);
    }

    public static Response<?> modelNotExists() {
        return ofFailed ("模型不存在", Response.ErrCode.MODEL_NOT_EXISTS);
    }

    public static Response<?> genericErr() {
        return ofFailed ("ERROR!", ErrCode.GENERIC_ERR);
    }

    public static <Q> Response<Q> genericErr(Q a) {
        return ofFailed (a, ErrCode.GENERIC_ERR);
    }

    public static <Q> Response<Q> serverErr(Q a) {
        return ofFailed (a, ErrCode.UNKNOWN_ERR);
    }

    public static Response<?> serverErr() {
        return ofFailed ("INTERNAL_SERVER_ERROR[emergency:contact fjz]", ErrCode.UNKNOWN_ERR);
    }

    //还有一种分类方式，
//    https://blog.csdn.net/qq_40610003/article/details/116587172
//    借助于properties文件
    @JsonSerialize(using = TypeSerializer.class)
    public static enum ErrCode {
        SUCCESS (0x0),
        UNKNOWN_ERR (0xffff_ffff), //服务器发生异常，如空指针异常等
        GENERIC_ERR (0xefff_ffff), //普通错误
        //model
        MODEL_NOT_EXISTS (0x1001),
        MODEL_ALREADY_STOPPED (0x1002),
        MODEL_IN_TRAINING (0x1003),
        //dataset
        DATASET_VALIDATION_FAILED (0x2001),
        DATASET_NOT_EXISTS (0x2002),
        //dataset
        ALGO_VALIDATION_FAILED (0x3001),
        ALGO_NOT_EXISTS (0x3002),
        //interact
        WRONG_DATASET_INPUT (0x4001);
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
}
