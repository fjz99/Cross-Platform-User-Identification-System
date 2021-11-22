package edu.nwpu.cpuis.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.nwpu.cpuis.utils.TypeSerializer;
import lombok.Data;

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

    public static <Q> Response<Q> fail(Q a) {
        return new Response<> (a, false, ErrCode.GENERIC_ERR);
    }

    public static <Q> Response<Q> of(Q a, boolean success, ErrCode errCode) {
        return new Response<> (a, success, errCode);
    }

    @JsonSerialize(using = TypeSerializer.class)
    public static enum ErrCode {
        SUCCESS (0x0),
        UNKNOWN_ERR (0xffff_ffff),
        GENERIC_ERR (0xefff_ffff),
        DATASET_VALIDATION_FAILED (0x01);
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
