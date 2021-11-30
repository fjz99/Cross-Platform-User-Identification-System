package edu.nwpu.cpuis.train.output;

import lombok.Data;

/**
 * 子类要精确规定output是什么
 */
@Data
public abstract class BaseOutput {
    protected String name;
    protected Boolean success;
    protected long time;//s
}
