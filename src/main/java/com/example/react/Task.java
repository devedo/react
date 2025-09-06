package com.example.react;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
@Data
public class Task implements Runnable {
	String name;
	Future<?> future;
	BiConsumer<Task, Double> progressTaskListener;
	MySimpMessagingTemplate messagingTemplate;

	public Task(final String name, final BiConsumer<Task, Double> progressTaskListener) {
		this.name = name;
		this.progressTaskListener = progressTaskListener;
	}

	@Override
	public void run() {
		try {
			final int cienPorciento = 100;
			for (int i = 1; (i <= cienPorciento && !Thread.currentThread().isInterrupted()); i++) {
				final long l = BigDecimal.valueOf(Math.random()).setScale(4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100 * (10 * (i % 2)))).longValue();
				TimeUnit.MILLISECONDS.sleep(l);
				this.getProgressTaskListener().accept(this, this.getPorcentage(i, cienPorciento));
			}
			if (Thread.currentThread().isInterrupted()) {
				log.info("Task {} interrupted", this.name);
				this.messagingTemplate.progress(NotificationProgress.builder().name(this.name)
						.type(NotificationProgress.NotificationType.IN_PROGRESS).build());
			} else {
				log.info("Task {} completed", this.name);
			}
		} catch (final InterruptedException e) {
			log.error("Task {} interrupted exceptio", this.name);
			throw new RuntimeException(e);
		}
	}

	public Task addFuture(final Future<?> submit) {
		this.future = submit;
		return this;
	}

	private Double getPorcentage(final int i, final int cienPorciento) {
		return BigDecimal.valueOf(i * 100L).divide(BigDecimal.valueOf(cienPorciento), 1, RoundingMode.HALF_DOWN)
				.setScale(1, RoundingMode.HALF_DOWN).doubleValue();
	}
}