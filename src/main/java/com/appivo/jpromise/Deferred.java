package com.appivo.jpromise;

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

    private State state = State.PENDING;
    private Object value;
    private Object error;
    private List<Function> onFullfilment;
    private List<Consumer> onRejection;

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

    public void resolve() {
	resolve(null);
    }

    public <T> void resolve(T t) {
	setState(State.RESOLVED);
	this.value = t;
	if (onFullfilment != null) {
	    for (Function resolve : onFullfilment) {
		execute(resolve, value);
	    }
	}
    }

    public void reject() {
	reject(null);
    }

    public <T> void reject(T t) {
	setState(State.REJECTED);
	this.error = t;
	if (onRejection != null) {
	    for (Consumer reject : onRejection) {
		execute(reject, error);
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
	return then((T value) -> {
	    return resolve.get();
	});
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
		return null;
	    }
	};
	Consumer<V> rej = (error) -> {
	    reject.accept(error);
	    def.reject(error);
	};
	if (resolve != null) {
	    addResolver(res);
	}
	if (reject != null) {
	    addRejector(reject);
	}
	return def;
    }

    private void execute(Object function, final Object value) {
	PromiseExecutor mgr = PromiseExecutor.getInstance();
	mgr.queue(() -> {
	    if (function instanceof Function) {
		((Function)function).apply(value);
	    } else if (function instanceof Consumer) {
		((Consumer)function).accept(value);
	    }
	});
    }

    private synchronized void addResolver(Function resolve) {
	if (onFullfilment == null) {
	    onFullfilment = new ArrayList<>();
	}
	if (isPending()) {
	    onFullfilment.add(resolve);
	} else if (isResolved()){
	    execute(resolve, value);
	}
    }

    private synchronized void addRejector(Consumer reject) {
	if (onRejection == null) {
	    onRejection = new ArrayList<>();
	}
	if (isPending()) {
	    onRejection.add(reject);
	} else if (isRejected()){
	    execute(reject, error);
	}
    }
}