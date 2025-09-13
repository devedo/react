package com.example.react.controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.example.react.MySimpMessagingTemplate;
import com.example.react.MyThreadPoolExecutor;
import com.example.react.NotificationProgress;
import com.example.react.NotificationProgress.NotificationType;
import com.example.react.PoolDetails;
import com.example.react.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/standardPool")
@RestController()
public class PoolController {
	private final MySimpMessagingTemplate messagingTemplate;

	private MyThreadPoolExecutor pool;
	private PoolDetails poolDetails;
	private boolean start;

	public void afterPropertiesSet() {
		this.pool.setProgressTaskHandler(this.getProgressTaskHandler());
		this.pool.setCancelledTaskHandler(this.getCancelledTaskHandler());
		this.pool.setDoneTaskHandler(this.getDoneTaskHandler());
		this.pool.setOtherRejectedTaskHandler(this.getRejectedExecutionHandler());
	}

	// Threads
	@PostMapping("/thread/add")
	public void standardPoolAdd(@RequestBody final Map<String, Object> objectMap) {
		log.info("add standardPoolAdd : {}", objectMap.get("name"));
		this.pool.submit(new Task((String) objectMap.get("name")));
	}
	@PostMapping("/thread/remove")
	public void standardPoolRemove(@RequestBody final Map<String, Object> objectMap) {
		log.info("remove standardPoolRemove : {}", objectMap.get("name"));
		this.pool.remove((String) objectMap.get("name"));
	}

	@PostMapping("/thread/stop")
	public void standardPoolStop(@RequestBody final Map<String, String> message) {
		log.info("stop standardPoolStop : {}", message);
		this.pool.remove(message.get("name"));
	}
	// Pool
	@GetMapping("/pool/details")
	public ResponseEntity<PoolDetails> standardPoolDetails() {
		log.info("details standardPoolDetails");
		return ResponseEntity.ok(this.pool == null ? PoolDetails.builder().build() : this.pool.details());
	}
	@GetMapping("/pool/shutDown")
	public ResponseEntity<Void> standardPoolShutDown() {
		log.info("shutDown standardPoolShutDown");
		this.pool.shutdown();
		return ResponseEntity.noContent().build();
	}
	@GetMapping("/pool/shutDownNow")
	public ResponseEntity<Void> standardPoolShutDownNow() {
		log.info("shutDown standardPoolShutDownNow");
		this.pool.shutdownNow();

		return ResponseEntity.noContent().build();
	}
	@PostMapping("/pool/change")
	public void standardPoolChange(@RequestBody final PoolDetails poolDetails) {
		log.info("rebuild standardPoolChange : {}", poolDetails.toString());
		if (this.pool != null)
			this.pool.shutdownNow();
		this.pool = null;
		try {

			this.pool = new MyThreadPoolExecutor(poolDetails.getCorePoolSize(), poolDetails.getMaximumPoolSize(),
					poolDetails.getKeepAliveTime(), TimeUnit.SECONDS, poolDetails.getWorkQueue());
			this.afterPropertiesSet();

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Handlers
	public BiConsumer<Task, Double> getProgressTaskHandler() {
		return (task, percentage) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName()).type(
					percentage >= 100D ? NotificationType.COMPLETED : NotificationProgress.NotificationType.IN_PROGRESS)
					.progress(percentage).build());
		};
	}

	public Consumer<Task> getCancelledTaskHandler() {
		return task -> {
			this.messagingTemplate.progress(
					NotificationProgress.builder().type(NotificationType.CANCELED).name(task.getName()).build());
		};
	}

	public Consumer<Task> getDoneTaskHandler() {
		return (task) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName())
					.type(NotificationType.COMPLETED).progress(100D).build());
		};
	}

	public Runnable getStatusPoolHandler() {
		final PoolDetails fin = this.poolDetails;
		return () -> {
			final PoolDetails update = this.pool.details();
			if (!update.equals(fin) || !this.start) {
				this.start = true;
				this.messagingTemplate.status(update);
				this.poolDetails = update;
			}
		};
	}

	private Consumer<Task> getRejectedExecutionHandler() {
		return (task) -> {
			this.messagingTemplate.progress(
					NotificationProgress.builder().type(NotificationType.REJECTED).name(task.getName()).build());
		};
	}

	@Scheduled(fixedRate = 1000)
	public void scheduledStatusPool() {
		if (this.pool != null)
			this.getStatusPoolHandler().run();
	}
}
