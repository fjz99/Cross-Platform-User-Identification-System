package org.cpuis.entity.exception;


import org.cpuis.entity.ErrCode;

public class CpuisException extends RuntimeException {
    private final ErrCode reason;
    private String data;

    public CpuisException(ErrCode reason) {
        super (reason.getMsg ());
        this.reason = reason;
    }

    public CpuisException(ErrCode reason, String data) {
        super (reason.getMsg ());
        this.reason = reason;
        this.data = data;
    }

    public ErrCode getReason() {
        return reason;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "super.toString () {" +
                "reason=" + reason +
                ", data='" + data + '\'' +
                '}';
    }
}
