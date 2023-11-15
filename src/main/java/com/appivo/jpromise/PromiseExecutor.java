package com.appivo.jpromise;

/**
 * An executor of promise callbacks. You can subclass this class and set your implementation using the setImplementation-method.
 *
 * @author Johan Eriksson
 */
public abstract class PromiseExecutor {
    private static PromiseExecutor instance;

    public static void setImplementation(PromiseExecutor inst) {
        instance = inst;
    }

    public static PromiseExecutor getInstance() {
        if (instance == null) {
            instance = new DefaultExecutor();
        }
        return instance;
    }

    protected void queue(Runnable task) {
        task.run();
    }

    protected void doShutdown() {
    }

    public final void shutdown() {
        doShutdown();
    }
}