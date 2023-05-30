package reactor.core.publisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

public class ThreadSwitchingFlux<T> extends Flux<T> implements Subscription, Runnable {

	private final ExecutorService executorService;
	private final T item;
	private CoreSubscriber<? super T> actual;
	AtomicBoolean done = new AtomicBoolean();

	public ThreadSwitchingFlux(T item, ExecutorService executorService) {
		this.item = item;
		this.executorService = executorService;
	}

	@Override
	public void subscribe(CoreSubscriber<? super T> actual) {
		this.actual = actual;
		this.executorService.submit(this);
	}

	@Override
	public void run() {
		this.actual.onSubscribe(this);
	}

	private void deliver() {
		if (done.compareAndSet(false, true)) {
			this.actual.onNext(this.item);
			this.actual.onComplete();
		}
	}

	@Override
	public void request(long n) {
		if (Operators.validate(n)) {
			if (!done.get()) {
				this.executorService.submit(this::deliver);
			}
		}
	}

	@Override
	public void cancel() {
		done.set(true);
	}
}