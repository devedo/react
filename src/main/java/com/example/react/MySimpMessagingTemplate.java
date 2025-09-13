package com.example.react;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class MySimpMessagingTemplate {
	private final SimpMessagingTemplate messagingTemplate;
	private final String topicProgress = "/topic/progress";
	private final String topicStatus = "/topic/status";

	public void progress(final NotificationProgress payload) {
		this.messagingTemplate.convertAndSend(this.topicProgress, payload);
	}
	public void status(final PoolDetails payload) {
		this.messagingTemplate.convertAndSend(this.topicStatus, payload);
	}

}