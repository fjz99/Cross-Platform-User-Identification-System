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
    private boolean success;

    private Response(T data, String status, boolean success) {
        timeStamp = System.currentTimeMillis ();
        this.data = data;
        this.msg = status;
        this.success = success;
    }

    public static <Q> Response<Q> ok(Q a) {
        return new Response<> (a, "success", true);
    }

    public static <Q> Response<Q> fail(Q a) {
        return new Response<> (a, "failed", false);
    }

    public static <Q> Response<Q> of(Q a, String msg, boolean success) {
        return new Response<> (a, msg, success);
    }
}
