package com.appivo.jpromise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultExecutor extends PromiseExecutor {
    ExecutorService executor;

    protected DefaultExecutor() {
	executor = Executors.newCachedThreadPool();
    }

    protected void queue(Runnable task) {
	executor.submit(task);
    }

    protected void doShutdown() {
	executor.shutdown();
    }
}
