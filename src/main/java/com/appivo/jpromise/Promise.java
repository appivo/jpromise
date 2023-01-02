package com.appivo.jpromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A promise very similar to a standard javascript promise
 *
 * @author Johan Eriksson
 */
public interface Promise {

    enum State {
	PENDING, RESOLVED, REJECTED
    }

    /**
     * Get the state of this promise
     * @return
     */
    State getState();

    /**
     * Get the value that this promise has resolved to. If the promise has not yet resolved this will return null.
     * @return
     * @param <T>
     */
    <T> T getValue();

    /**
     * Check if this promise is still pending.
     * @return
     */
    boolean isPending();

    /**
     * Check if this promise is resolved
     * @return
     */
    boolean isResolved();

    /**
     * Check if this promise is rejected
     * @return
     */
    boolean isRejected();

    /**
     * Add a callback for when this promise has resolved.
     *
     * @param resolve Function to invoke when this promise has been resolved
     * @return A new promise
     * @param <T>
     * @param <U>
     */
    <T,U> Promise then(Function<T, U> resolve);

    <T> Promise then(Consumer<T> resolve);

    <T> Promise then(Runnable resolve);

    <T> Promise then(Supplier<T> resolve);

    <T,V> Promise then(Consumer<T> resolve, Consumer<V> reject);

    <T,V> Promise then(Runnable resolve, Consumer<V> reject);

    <T,V> Promise then(Supplier<T> resolve, Consumer<V> reject);

    <T,U,V> Promise then(Function<T, U> resolve, Consumer<V> reject);

    public static <T> Promise resolve(T value) {
	Deferred def = new Deferred();
        def.resolve(value);
        return def;
    }

    public static <T> Promise reject(T error) {
        Deferred def = new Deferred();
        def.reject(error);
        return def;
    }

    /**
     * Create a promise that resolves when all of the provided promises have been successfully resolved. If any of the promises gets rejected this promise will also be rejected.
     * @param promises
     * @return
     * @param <T>
     */
    public static <T> Promise all(Promise... promises) {
        final List<T> values = new ArrayList<>();
        return multi((value, count) -> {
            values.add((T)value);
            if (count.getAndDecrement() == 0) {
                return new Response(State.RESOLVED, values.toArray());
            } else {
                return new Response(State.PENDING, null);
            }
        }, (err) -> {
            return new Response(State.REJECTED, err);
        }, promises);
    }

    public static <T> Promise any(Promise... promises) {
        return multi((value, count) -> {
            return new Response(State.RESOLVED, value);
        }, (err) -> {
            return new Response(State.REJECTED, err);
        }, promises);
    }

    public static <T> Promise race(Promise... promises) {
        return multi((value, count) -> {
            return new Response(State.RESOLVED, value);
        }, (err) -> {
            return new Response(State.RESOLVED, err);
        }, promises);
    }

    static <T> Promise multi(BiFunction<T, AtomicInteger, Response<T>> onResolve, Function<Object, Response<Object>> onReject, Promise... promises) {
        Deferred def = new Deferred();
        final AtomicInteger count = new AtomicInteger(promises.length);
        for (Promise promise : promises) {
            promise.then((T value) -> {
                Response<T> response = onResolve.apply(value, count);
                if (response.state == State.RESOLVED) {
                    def.resolve(response.value);
                }
            }, (err) -> {
                Response<Object> response = onReject.apply(err);
                if (response.state == State.REJECTED) {
                    def.reject(response.value);
                }
            });
        }
        return def;
    }

    static class Response<T> {
        State state;
        T value;

        Response(State state, T value) {
            this.state = state;
            this.value = value;
        }
    }
}
