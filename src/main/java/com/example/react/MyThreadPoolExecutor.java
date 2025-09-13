package com.example.react;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unchecked")
@Slf4j
@Getter
@Setter
public class MyThreadPoolExecutor extends ThreadPoolExecutor {

	private Consumer<Task> cancelledTaskHandler;
	private Consumer<Task> doneTaskHandler;
	private BiConsumer<Task, Double> progressTaskHandler;
	private Consumer<Task> otherRejectedTaskHandler;
	private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();
	private final AtomicInteger rejectedTasks = new AtomicInteger();

	private static PoolDetails poolDetails = PoolDetails.builder().corePoolSize(1).maximumPoolSize(2).keepAliveTime(0)
			.workQueue(1).build();

	public MyThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final Integer keepAliveTime,
			final TimeUnit unit, final Integer workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, createQueue(workQueue), createThreadFactory());
		poolDetails = poolDetails.toBuilder().workQueue(workQueue < 1 ? null : workQueue).corePoolSize(corePoolSize)
				.maximumPoolSize(maximumPoolSize).keepAliveTime(keepAliveTime).build();
	}

	private static BlockingQueue<Runnable> createQueue(final Integer integer) {
		if (integer > 0)
			return new ArrayBlockingQueue<>(integer);
		else if (integer == 0)
			return new LinkedBlockingQueue<>();
		else if (integer == -1)
			return new SynchronousQueue<>();

		throw new RuntimeException();
	}

	@Override
	protected void beforeExecute(final Thread t, final Runnable r) {
		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(final Runnable r, final Throwable t) {
		super.afterExecute(r, t);
		this.tasks.stream().filter(entry -> entry.getFuture().equals(r)).findAny().ifPresent(entry -> {
			if (((FutureTask) r).isCancelled())
				this.getCancelledTaskHandler().accept(entry);
			else if (((FutureTask) r).isDone())
				this.getDoneTaskHandler().accept(entry);
			this.tasks.remove(entry);
		});
	}

	public void remove(final String name) {
		this.tasks.stream().filter(task1 -> task1.getName().equals(name)).findAny().ifPresent(task -> {
			task.getFuture().cancel(true);
			this.cancelledTaskHandler.accept(task);
			this.getQueue().remove(task.getFuture());
		});
	}

	public PoolDetails details() {
		return this.details(poolDetails);
	}
	public <T extends PoolDetails> T details(final T poolDetails) {
		return (T) poolDetails.toBuilder().workQueue(poolDetails.getWorkQueue()).corePoolSize(this.getCorePoolSize()) // número
																														// de
																														// hilos
																														// básicos
				// (core threads) que
				// mantiene el grupo.
				.maximumPoolSize(this.getMaximumPoolSize())// tamaño máximo permitido del grupo de hilos.
				.poolSize(this.getPoolSize()) // número actual de hilos en el grupo.
				.activeCount(this.getActiveCount())// estimación aproximada del número de hilos que están ejecutando
				// activamente tareas.
				.completedTaskCount(this.getCompletedTaskCount())// cuenta aproximada de tareas completadas por los
				// hilos en elgrupo.
				.taskCount(this.getTaskCount())// cuenta aproximada de tareas totales que han sido programadas para
				// ejecución.
				.runingQueue(this.getQueue().size()) // el número de tareas actualmente en la cola.
				.isShutdown(this.isShutdown()) // true si el ejecutor ha sido apagado.
				.isTerminating(this.isTerminating()) // true si el ejecutor está en proceso de apagarse pero no ha
				// terminado completamente.
				.isTerminated(this.isTerminated()) // true si el ejecutor ha terminado.
				.rejected(this.rejectedTasks.get()) // true si el ejecutor ha terminado.
				.build();
	}

	@Override
	public void shutdown() {
		super.shutdown();
	}
	@Override
	protected void terminated() {
		super.terminated();
		log.info("terminated");
	}
	@Override
	public List<Runnable> shutdownNow() {
		final List<Runnable> runnables = super.shutdownNow();
		return runnables;
	}

	public void submit(final Task task) {
		log.info("submit : {}", task);
		task.setProgressTaskListener(this.progressTaskHandler);
		try {
			task.setFuture(super.submit(task));
			this.tasks.add(task);
		} catch (final Exception e) {
			this.rejectedTasks.incrementAndGet();
			this.getOtherRejectedTaskHandler().accept(task);
		}
	}
	private static ThreadFactory createThreadFactory() {
		return new ThreadFactory() {
			private int threadCount = 1;

			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r);
				t.setName("MyTheadPoolExecutor thread: " + this.threadCount++); // Nombre personalizado
				t.setDaemon(false); // No es un hilo demonio
				t.setPriority(Thread.NORM_PRIORITY); // Prioridad normal
				System.out.println("Hilo creado: " + t.getName());
				return t;
			}
		};
	}

}