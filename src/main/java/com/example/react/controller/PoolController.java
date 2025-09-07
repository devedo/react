package com.example.react.controller;

import com.example.react.*;
import com.example.react.NotificationProgress.NotificationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/standardPool")
@RestController()
public class PoolController {
	private final MySimpMessagingTemplate messagingTemplate;

	private final PoolDetails poolDetails = PoolDetails.builder().corePoolSize(3).maximumPoolSize(5).keepAliveTime(0)
			.deque(5).build();

	private MyThreadPoolExecutor pool = new MyThreadPoolExecutor(this.poolDetails.getCorePoolSize(),
			this.poolDetails.getMaximumPoolSize(), this.poolDetails.getKeepAliveTime(), TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(this.poolDetails.getDeque()));

	@PostConstruct
	public void init() {
		this.pool.setProgressTaskListener(this.getProgressTaskHandler());
		this.pool.setCancelledTaskListener(this.getCancelledTaskHandler());
		this.pool.setDoneTaskListener(this.getDoneTaskHandler());

		this.pool.setRejectedExecutionHandler((r, executor) -> {
			log.warn("Task {} rejected from {}", r.toString(), executor.toString());
		});
	}

	@GetMapping("/details")
	public ResponseEntity<PoolDetails> standardPoolDetails() {
		log.info("details standardPoolDetails : {}", LocalDateTime.now());
		return ResponseEntity.ok(this.poolDetails);
	}

	@PostMapping("/add")
	public void standardPoolAdd(@RequestBody final Map<String, Object> objectMap) {
		log.info("add standardPoolAdd : {}", objectMap.get("name"));
		this.pool.submit(new Task((String) objectMap.get("name"), this.getProgressTaskHandler()));
	}
	@PostMapping("/remove")
	public void standardPoolRemove(final String name) {
		log.info("remove standardPoolRemove : {}", name);
		this.pool.remove(name);
	}

	@PostMapping("/stop")
	public void standardPoolStop(@RequestBody final Map<String, String> message) {
		log.info("stop standardPoolStop : {}", message);
		this.pool.remove(message.get("name"));
	}
	@PostMapping("/rebuild")
	public void standardPoolRebuild(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime) {
		log.info("rebuild standardPoolRebuild : {} {} {}", corePoolSize, maximumPoolSize, keepAliveTime);
		this.pool.shutdownNow();
		this.pool = new MyThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(this.poolDetails.getDeque()));
	}

	public BiConsumer<Task, Double> getProgressTaskHandler() {
		return (task, percentage) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName()).type(
					percentage >= 100D ? NotificationType.COMPLETED : NotificationProgress.NotificationType.IN_PROGRESS)
					.progress(percentage).build());
			this.messagingTemplate.status(this.pool.details());
		};
	}

	public Consumer<Task> getCancelledTaskHandler() {
		return task -> {
			this.messagingTemplate.progress(
					NotificationProgress.builder().type(NotificationType.CANCELED).name(task.getName()).build());
			this.messagingTemplate.status(this.pool.details());
		};
	}

	public Consumer<Task> getDoneTaskHandler() {
		return (task) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName())
					.type(NotificationType.COMPLETED).progress(100D).build());
			this.messagingTemplate.status(this.pool.details());
		};
	}
}
