package edu.nwpu.cpuis.entity.exception;


import edu.nwpu.cpuis.entity.ErrCode;

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
}
