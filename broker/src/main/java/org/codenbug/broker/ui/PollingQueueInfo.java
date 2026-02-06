package org.codenbug.broker.ui;

import lombok.Getter;

@Getter
public class PollingQueueInfo {
	private String state;
	private Long rank;
	private String entryAuthToken;
	private long pollAfterMs;

	protected PollingQueueInfo() {
	}

	public PollingQueueInfo(String state, Long rank, String entryAuthToken, long pollAfterMs) {
		this.state = state;
		this.rank = rank;
		this.entryAuthToken = entryAuthToken;
		this.pollAfterMs = pollAfterMs;
	}
}
