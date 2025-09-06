package com.example.react;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PoolDetails {
	protected Integer corePoolSize;
	protected Integer maximumPoolSize;
	protected Integer keepAliveTime;
	protected Integer deque;
	protected Integer poolSize;
	protected Integer activeCount;
	protected Integer queue;
	protected Boolean isShutdown;
	protected Boolean isTerminating;
	protected Boolean isTerminated;

	protected long completedTaskCount;
	protected long taskCount;

}
