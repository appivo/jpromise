package com.appivo.jpromise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An executor of promise callbacks. You can subclass this class and set your implementation using the setImplementation-method.
 *
 * @author Johan Eriksson
 */
public class PromiseExecutor {
    private static PromiseExecutor instance;

    ExecutorService executor;

    protected PromiseExecutor() {
        executor = Executors.newWorkStealingPool();
    }

    protected void queue(Runnable task) {
        executor.submit(task);
    }

    public final static void setImplementation(PromiseExecutor inst) {
        instance = inst;
    }

    static PromiseExecutor getInstance() {
        if (instance == null) {
            instance = new PromiseExecutor();
        }
        return instance;
    }

    protected void doShutdown() {
        executor.shutdown();
    }

    public final void shutdown() {
        doShutdown();
    }
}