package com.example.react;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Data
public class Task implements Runnable {
	String name;
	@ToString.Exclude
	Future<?> future;
	@ToString.Exclude
	BiConsumer<Task, Double> progressTaskListener;

	public Task(final String name) {
		this.name = name;
	}

	@Override
	public void run() {
		try {
			final int cienPorciento = 100;
			this.getProgressTaskListener().accept(this, 0D);
			if (!Thread.currentThread().isInterrupted()) {
				for (int i = 1; (i <= cienPorciento && !Thread.currentThread().isInterrupted()); i++) {
					final long l = BigDecimal.valueOf(Math.random()).setScale(4, RoundingMode.HALF_UP)
							.multiply(BigDecimal.valueOf(100 * (10 * (i % 2)))).longValue();
					TimeUnit.MILLISECONDS.sleep(l);
					final Double porcentage = this.getPorcentage(i, cienPorciento);
					System.out.print(this.name + ":[" + porcentage + "] ");
					this.getProgressTaskListener().accept(this, porcentage);
				}
				System.out.println();
			} else {
				log.info("Task {} completed", this.name);
			}
		} catch (final InterruptedException e) {
			log.error("Task {} interrupted exception", this.name);
			throw new RuntimeException(e);
		}
	}

	private Double getPorcentage(final int i, final int cienPorciento) {
		return BigDecimal.valueOf(i * 100L).divide(BigDecimal.valueOf(cienPorciento), 1, RoundingMode.HALF_DOWN)
				.setScale(1, RoundingMode.HALF_DOWN).doubleValue();
	}

}