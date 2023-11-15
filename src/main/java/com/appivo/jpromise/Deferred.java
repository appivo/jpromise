package com.appivo.jpromise;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A deferred
 *
 * @author Johan Eriksson
 */
public class Deferred implements Promise {

    protected enum InvocationType {
	RESOLVE, REJECT
    }

    private State state = State.PENDING;
    private Object value;
    private Object error;
    private List<Function<?,?>> onFullfilment;
    private List<Consumer<?>> onRejection;

    public Deferred() {
    }

    @Override
    public final boolean isPending() {
	return state == State.PENDING;
    }

    @Override
    public final boolean isResolved() {
	return state == State.RESOLVED;
    }

    @Override
    public final boolean isRejected() {
	return state == State.REJECTED;
    }

    @Override
    public final <T> T getValue() {
	return (T)value;
    }

    /**
     * Resolve this deferred without a value
     */
    public void resolve() {
	resolve(null);
    }

    /**
     * Resolve this deferred with a value
     * @param value Resolved value
     * @param <T>
     */
    public <T> void resolve(T value) {
	setState(State.RESOLVED);
	this.value = value;
	executeResolve(value);
    }

    protected <T> void executeResolve(T value) {
	boolean done = false;
	if (value instanceof Value) {
	    Value val = (Value)value;
	    if (val.hasMember("then") && val.canInvokeMember("then")) {
		done = true;
		val.invokeMember("then", (Consumer)this::executeResolve, (Consumer)this::executeReject);
	    }
	}
	if (!done) {
	    if (value instanceof Promise) {
		Promise p = (Promise) value;
		p.then((Consumer) this::executeResolve, (Consumer) this::executeReject);
	    } else {
		if (onFullfilment != null) {
		    for (Function resolve : onFullfilment) {
			execute(InvocationType.RESOLVE, resolve, value);
		    }
		}
	    }
	}
    }

    /**
     * Reject this deferred
     */
    public void reject() {
	reject(null);
    }

    /**
     * Resolve this deferred with an error
     * @param error Error
     * @param <T>
     */
    public <T> void reject(T error) {
	setState(State.REJECTED);
	this.error = error;
	executeReject(error);
    }

    protected <T> void executeReject(T error) {
	if (onRejection != null) {
	    for (Consumer reject : onRejection) {
		execute(InvocationType.REJECT, reject, error);
	    }
	}
    }

    @Override
    public final State getState() {
	return state;
    }

    private void setState(State state) {
	if (isPending() && state == State.RESOLVED || state == State.REJECTED) {
	    this.state = state;
	} else {
	    if (state != State.PENDING) {
		throw new RuntimeException("Promise has already been " + (state == State.RESOLVED ? "resolved" : "rejected"));
	    }
	}
    }

    @Override
    public <T,U> Promise then(Function<T,U> resolve) {
	return then(resolve, null);
    }

    @Override
    public <T> Promise then(Consumer<T> resolve) {
	return then((T value) -> {
	    resolve.accept(value);
	    return null;
	});
    }

    @Override
    public <T> Promise then(Runnable resolve) {
	return then((T value) -> {
	    resolve();
	    return value;
	});
    }

    @Override
    public <T> Promise then(Supplier<T> resolve) {
	return then((T value) -> resolve.get());
    }

    @Override
    public <T, V> Promise then(Consumer<T> resolve, Consumer<V> reject) {
	return then((T value) -> {
	    resolve.accept(value);
	    return null;
	}, reject);
    }

    @Override
    public <T, V> Promise then(Runnable resolve, Consumer<V> reject) {
	return then((T value) -> {
	    resolve.run();
	    return value;
	}, reject);
    }

    @Override
    public <T, V> Promise then(Supplier<T> resolve, Consumer<V> reject) {
	return then((T value) -> {
	    value = resolve.get();
	    return value;
	}, reject);
    }

    @Override
    public <T,U, V> Promise then(Function<T,U> resolve, Consumer<V> reject) {
	Deferred def = new Deferred();
	Function<T,U> res = (value) -> {
	    try {
		U val = resolve.apply(value);
		def.resolve(val);
		return val;
	    } catch (Throwable err) {
		def.reject(err);
		handleError(InvocationType.RESOLVE, resolve, err);
		return null;
	    }
	};
	Consumer<V> rej = (error) -> {
	    try {
		reject.accept(error);
	    } catch (Throwable err) {
		handleError(InvocationType.REJECT, reject, err);
	    }
	    def.reject(error);
	};
	if (resolve != null) {
	    addResolver(res);
	}
	if (reject != null) {
	    addRejector(rej);
	}
	return def;
    }

    private void execute(InvocationType type, Object function, final Object value) {
	PromiseExecutor mgr = PromiseExecutor.getInstance();
	mgr.queue(() -> {
	    invokeCallback(type, function, value);
	});
    }

    protected void invokeCallback(InvocationType type, Object function, final Object value) {
	preCallback(type, function, value);
	try {
	    if (function instanceof Function) {
		Object val = ((Function) function).apply(value);
	    } else if (function instanceof Consumer) {
		((Consumer) function).accept(value);
	    }
	} finally {
	    postCallback(type, function, value);
	}
    }

    protected void preCallback(InvocationType type, final Object function, final Object value) {
    }

    protected void postCallback(InvocationType type, final Object function, final Object value) {
    }

    /**
     * Handle an error that occured while invoking a callback
     * @param type Type of invocation
     * @param function callback
     * @param error Error
     */
    protected void handleError(InvocationType type, Object function, Throwable error) {
    }

    protected void resolveAdded(Function resolve) {
    }

    protected void rejectAdded(Consumer reject) {
    }

    private synchronized void addResolver(Function resolve) {
	if (onFullfilment == null) {
	    onFullfilment = new ArrayList<>();
	}
	if (isPending()) {
	    resolveAdded(resolve);
	    onFullfilment.add(resolve);
	} else if (isResolved()){
	    invokeCallback(InvocationType.RESOLVE, resolve, value);
	}
    }

    private synchronized void addRejector(Consumer reject) {
	if (onRejection == null) {
	    onRejection = new ArrayList<>();
	}
	if (isPending()) {
	    rejectAdded(reject);
	    onRejection.add(reject);
	} else if (isRejected()){
	    invokeCallback(InvocationType.REJECT, reject, error);
	}
    }
}