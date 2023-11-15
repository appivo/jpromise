package com.appivo.jpromise;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest {
    @Test
    public void testSimpleResolve() {
        Promise promise = Promise.resolve(5);
        assertFalse(promise.isPending());
        assertTrue(promise.isResolved());
        assertFalse(promise.isRejected());
        assertEquals(promise.getState(), Promise.State.RESOLVED);
        assertEquals((int)promise.getValue(), 5);
    }

    @Test
    public void testSimpleReject() {
        Exception e = new Exception();
        Promise promise = Promise.reject(e);
        assertFalse(promise.isPending());
        assertTrue(promise.isRejected());
        assertFalse(promise.isResolved());
        assertEquals(promise.getState(), Promise.State.REJECTED);
    }

    @Test
    public void testPending() {
        Deferred def = new Deferred();
        assertTrue(def.isPending());
        assertFalse(def.isRejected());
        assertFalse(def.isResolved());
        assertEquals(def.getState(), Promise.State.PENDING);
    }

    @Test
    public void testResolve() {
        try {
            Deferred def = new Deferred();
            CompletableFuture<String> future = new CompletableFuture<>();
            def.then((value) -> {
                future.complete((String) value);
            });
            def.resolve("Hello World");
            assertEquals(future.get(), "Hello World");
            assertTrue(def.isResolved());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testDependentResolve() {
        try {
            CountDownLatch lock = new CountDownLatch(1);
            ExecutorService exec = Executors.newCachedThreadPool();
            Deferred def = new Deferred();
            Deferred def2 = new Deferred();
            final AtomicInteger fval = new AtomicInteger();
            def2.then((value) -> (Integer)value + 8).then((value) -> {
                exec.submit(() -> {
                    def.resolve(((Integer)value) + 8);
                    fval.set((Integer)value);
                    lock.countDown();
                });
            });
            def2.resolve(5);
            lock.await(1000, TimeUnit.MILLISECONDS);
            assertEquals(13, fval.get());
            assertTrue(def.isResolved());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testChainedResolve() {
        try {
            CountDownLatch lock = new CountDownLatch(1);
            ExecutorService exec = Executors.newCachedThreadPool();
            Deferred def = new Deferred();
            final AtomicInteger fval = new AtomicInteger();
            def.then((value) -> (Integer)value + 8).then((value) -> {
                Deferred def2 = new Deferred();
                exec.submit(() -> {
                    def2.resolve(((Integer)value) + 8);
                });
                return def2;
            }).then((val2) -> {
                fval.set((Integer)val2);
                lock.countDown();
            });
            def.resolve(5);
            lock.await(1000, TimeUnit.MILLISECONDS);
            assertEquals(21, fval.get());
            assertTrue(def.isResolved());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testChainedResolve2() {
        try {
            Deferred def = new Deferred();
            CompletableFuture<Integer> fval = new CompletableFuture<>();
            def.then((value) -> (Integer)value + 8).then((value) -> {
                return ((Integer)value) + 3;
            }).then((val2) -> {
                return ((Integer)val2) + 7;
            }).then((val3) -> {
                return ((Integer)val3) + 5;
            }).then(val4 -> {
                fval.complete((Integer)val4);
            });
            def.resolve(5);
            assertEquals(28, fval.get());
            assertTrue(def.isResolved());
        } catch (Exception e) {
            fail(e);
        }
    }
}