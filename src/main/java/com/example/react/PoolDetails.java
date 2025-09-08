package com.example.react;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class PoolDetails {
	protected Integer maximumPoolSize;
	protected Integer corePoolSize;
	protected Integer keepAliveTime;
	protected Integer workQueue;
	protected long taskCount;
	protected long completedTaskCount;
	protected Integer activeCount;
	protected Integer poolSize;
	protected Integer runingQueue;
	protected Boolean isShutdown;
	protected Boolean isTerminating;
	protected Boolean isTerminated;
}
