package org.codenbug.broker.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerQueueObservationTest {

	@Test
	void recordsPollingAndQueueMetrics() {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		WaitingQueueRedisRepository repository = mock(WaitingQueueRedisRepository.class);
		when(repository.countActiveEntryTokens()).thenReturn(3L);
		MicrometerQueueObservation observation = new MicrometerQueueObservation(meterRegistry, repository);

		observation.recordPollingRequest("event-1", "WAITING", 5000L);
		observation.recordQueueState("event-1", 120L, 8L);

		assertThat(meterRegistry.counter("ticketon.queue.polling.requests",
			"eventId", "event-1", "state", "WAITING").count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("ticketon.queue.polling.last_delay_ms")
			.tag("eventId", "event-1")
			.tag("state", "WAITING")
			.gauge()
			.value()).isEqualTo(5000.0);
		assertThat(meterRegistry.get("ticketon.queue.waiting_users").tag("eventId", "event-1")
			.gauge().value()).isEqualTo(120.0);
		assertThat(meterRegistry.get("ticketon.queue.entry_slots").tag("eventId", "event-1")
			.gauge().value()).isEqualTo(8.0);
		assertThat(meterRegistry.get("ticketon.queue.active_tokens").gauge().value()).isEqualTo(3.0);
	}
}
