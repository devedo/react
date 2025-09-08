package com.example.react;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class NotificationProgress extends PoolDetails {
	protected String name;
	protected NotificationType type;
	protected Double progress;

	public enum NotificationType {
		INTERRUPTED, IN_PROGRESS, COMPLETED, CANCELED, FAILED, REJECTED
	}
}