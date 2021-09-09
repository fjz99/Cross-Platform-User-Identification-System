package edu.nwpu.cpuis.network;

/**
 * 连接的建立和关闭需要自己进行
 * @author fujiazheng
 */
@FunctionalInterface
public interface ModelClient {
    /**
     * 这个方法是阻塞等待响应的
     * @param req 请求
     * @return 响应
     * @throws InterruptedException
     */
    Package send(Package req) throws InterruptedException;
}
