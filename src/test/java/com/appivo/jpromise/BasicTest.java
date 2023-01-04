package com.appivo.jpromise;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

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
    public void testChainedResolve() {
        try {
            Deferred def = new Deferred();
            CompletableFuture<Integer> future = new CompletableFuture<>();
            def.then((value) -> (Integer)value + 8).then((value) -> {
                return future.complete((Integer) value);
            });
            def.resolve(5);
            assertEquals(future.get(), 13);
            assertTrue(def.isResolved());
        } catch (Exception e) {
            fail(e);
        }
    }
}