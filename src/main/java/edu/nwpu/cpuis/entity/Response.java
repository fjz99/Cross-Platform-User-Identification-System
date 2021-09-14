package edu.nwpu.cpuis.entity;

import lombok.Data;

/**
 * @author fujiazheng
 */
@Data
public final class Response<T> {
    private final long timeStamp;
    private T data;
    private String msg;

    private Response(T data, String status) {
        timeStamp = System.currentTimeMillis ();
        this.data = data;
        this.msg = status;
    }

    public static <Q> Response<Q> ok(Q a) {
        return new Response<> (a, "success");
    }

    public static <Q> Response<Q> fail(Q a) {
        return new Response<> (a, "failed");
    }

    public static <Q> Response<Q> of(Q a, String msg) {
        return new Response<> (a, msg);
    }
}
