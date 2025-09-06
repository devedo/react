package com.example.react.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class EventController {

	private final SimpMessagingTemplate messagingTemplate;

	// Recibe un mensaje en /app/notify
	@MessageMapping("/notify")
	public void sendNotification(final String message) {
		// Envía a todos los suscriptores de /topic/notifications
		final LocalDateTime now = LocalDateTime.now();
		// Notification notification = Notification.builder().message("Notificación
		// automática #" + now).build();

		// messagingTemplate.convertAndSend("/topic/notifications", notification);
	}
}
