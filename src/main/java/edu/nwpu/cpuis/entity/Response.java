package edu.nwpu.cpuis.entity;

import lombok.Getter;

@Getter
public final class Response<T> {
    private static final Response<?> GENERIC_ERR = ofFailed ("ERROR!", ErrCode.GENERIC_ERR);
    private static final Response<?> MODEL_NOT_EXISTS = ofFailed ("模型不存在", ErrCode.MODEL_NOT_EXISTS);
    private static final Response<?> INTERNAL_SERVER_ERR = ofFailed ("INTERNAL_SERVER_ERROR", ErrCode.UNKNOWN_ERR);
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
        return MODEL_NOT_EXISTS;
    }

    public static Response<?> genericErr() {
        return GENERIC_ERR;
    }

    public static <Q> Response<Q> genericErr(Q a) {
        return ofFailed (a, ErrCode.GENERIC_ERR);
    }

    public static <Q> Response<Q> serverErr(Q a) {
        return ofFailed (a, ErrCode.UNKNOWN_ERR);
    }

    public static Response<?> serverErr() {
        return INTERNAL_SERVER_ERR;
    }

    @Override
    public String toString() {
        return "Response{" +
                "timeStamp=" + timeStamp +
                ", data=" + "masked" +
                ", success=" + success +
                ", err=" + err +
                '}';
    }
}
