package edu.nwpu.cpuis.network;

public interface ModelClient {
    void shutdown() throws InterruptedException;

    void start() throws Exception;

    Package send(Package req) throws InterruptedException;
}
