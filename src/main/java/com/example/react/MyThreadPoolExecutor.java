package com.example.react;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class MyThreadPoolExecutor extends ThreadPoolExecutor {

	private Runnable statusPoolHandler;
	private Consumer<Task> cancelledTaskHandler;
	private Consumer<Task> doneTaskHandler;
	private BiConsumer<Task, Double> progressTaskHandler;
	private final List<Task> tasks = new ArrayList<>();

	public MyThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime,
			final TimeUnit unit, final BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public void setRejectedExecutionHandler(final Consumer<Task> rejectedTaskHandler) {
		this.setRejectedExecutionHandler((r, poll) -> {
			System.out.println("->>>>>>>>>>>>>>>>>>>>>>>>>>>>rejectedTaskHandler");
			this.tasks.stream().filter(entry -> entry.getFuture().equals(r)).findAny().ifPresent(rejectedTaskHandler);

		});

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
		this.getStatusPoolHandler().run();
	}

	public void remove(final String name) {
		this.tasks.stream().filter(task1 -> task1.getName().equals(name)).findAny().ifPresent(task -> {
			task.getFuture().cancel(true);
			this.cancelledTaskHandler.accept(task);
			this.getQueue().remove(task.getFuture());
		});
	}
	public PoolDetails details() {
		final PoolDetails build = PoolDetails.builder().corePoolSize(this.getCorePoolSize()) // número de hilos básicos
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
				.build();

		return build;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		this.getStatusPoolHandler().run();
	}
	@Override
	protected void terminated() {
		super.terminated();
		this.getStatusPoolHandler().run();
		log.info("terminated");
	}
	@Override
	public List<Runnable> shutdownNow() {
		final List<Runnable> runnables = super.shutdownNow();
		this.getStatusPoolHandler().run();
		return runnables;
	}

	public void submit(final Task task) {
		log.info("submit : {}", task);
		this.tasks.add(task.addFuture(super.submit(task)).setProgressTaskListener(this.progressTaskHandler));
	}

}