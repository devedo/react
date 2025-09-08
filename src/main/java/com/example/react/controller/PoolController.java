package com.example.react.controller;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/standardPool")
@RestController()
public class PoolController implements InitializingBean {
	private final MySimpMessagingTemplate messagingTemplate;

	private final PoolDetails poolDetails = PoolDetails.builder().corePoolSize(1).maximumPoolSize(2).keepAliveTime(0)
			.workQueue(1).build();

	private MyThreadPoolExecutor pool = new MyThreadPoolExecutor(this.poolDetails.getCorePoolSize(),
			this.poolDetails.getMaximumPoolSize(), this.poolDetails.getKeepAliveTime(), TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(this.poolDetails.getWorkQueue()));

	@Override
	public void afterPropertiesSet() {
		this.messagingTemplate.setStatusPoolHandler(this.getStatusPoolHandler());
		this.pool.setProgressTaskHandler(this.getProgressTaskHandler());
		this.pool.setCancelledTaskHandler(this.getCancelledTaskHandler());
		this.pool.setDoneTaskHandler(this.getDoneTaskHandler());
		this.pool.setRejectedExecutionHandler(this.getRejectedExecutionHandler());
		this.pool.setStatusPoolHandler(this.getStatusPoolHandler());
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
		return ResponseEntity.ok(this.pool.details());
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
		this.pool.shutdownNow();

		this.pool = new MyThreadPoolExecutor(poolDetails.getCorePoolSize(), poolDetails.getMaximumPoolSize(),
				poolDetails.getKeepAliveTime(), TimeUnit.SECONDS,
				poolDetails.getWorkQueue() != 0
						? new ArrayBlockingQueue<>(poolDetails.getWorkQueue())
						: new LinkedBlockingQueue<>());
		try {
			this.afterPropertiesSet();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		this.getStatusPoolHandler().run();
	}

	// Handlers
	public BiConsumer<Task, Double> getProgressTaskHandler() {
		return (task, percentage) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName()).type(
					percentage >= 100D ? NotificationType.COMPLETED : NotificationProgress.NotificationType.IN_PROGRESS)
					.progress(percentage).build());
			this.getStatusPoolHandler().run();
		};
	}

	public Consumer<Task> getCancelledTaskHandler() {
		return task -> {
			this.messagingTemplate.progress(
					NotificationProgress.builder().type(NotificationType.CANCELED).name(task.getName()).build());
			this.getStatusPoolHandler().run();
		};
	}

	public Consumer<Task> getDoneTaskHandler() {
		return (task) -> {
			this.messagingTemplate.progress(NotificationProgress.builder().name(task.getName())
					.type(NotificationType.COMPLETED).progress(100D).build());
			this.getStatusPoolHandler().run();
		};
	}
	public Runnable getStatusPoolHandler() {
		return () -> this.messagingTemplate.status(this.pool.details());
	}

	private Consumer<Task> getRejectedExecutionHandler() {
		return (task) -> {
			this.messagingTemplate.progress(
					NotificationProgress.builder().type(NotificationType.REJECTED).name(task.getName()).build());
			this.getStatusPoolHandler().run();
		};
	}

}
