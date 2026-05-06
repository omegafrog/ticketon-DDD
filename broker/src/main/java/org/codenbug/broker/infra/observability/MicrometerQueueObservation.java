package org.codenbug.broker.infra.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.codenbug.broker.app.QueueObservation;
import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@Component
public class MicrometerQueueObservation implements QueueObservation {
	private final MeterRegistry meterRegistry;
	private final WaitingQueueRedisRepository waitingQueueRedisRepository;
	private final Map<String, AtomicLong> waitingUsersByEvent = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> entrySlotsByEvent = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> pollingDelayByEventState = new ConcurrentHashMap<>();

	public MicrometerQueueObservation(MeterRegistry meterRegistry,
		WaitingQueueRedisRepository waitingQueueRedisRepository) {
		this.meterRegistry = meterRegistry;
		this.waitingQueueRedisRepository = waitingQueueRedisRepository;
		Gauge.builder("ticketon.queue.active_tokens", waitingQueueRedisRepository,
				WaitingQueueRedisRepository::countActiveEntryTokens)
			.description("Active queue entry token count")
			.register(meterRegistry);
	}

	@Override
	public void recordPollingRequest(String eventId, String state, long pollAfterMs) {
		Counter.builder("ticketon.queue.polling.requests")
			.description("Polling queue API request count")
			.tags("eventId", eventId, "state", state)
			.register(meterRegistry)
			.increment();
		pollingDelayByEventState.computeIfAbsent(eventId + ":" + state, ignored -> {
			AtomicLong value = new AtomicLong();
			Gauge.builder("ticketon.queue.polling.last_delay_ms", value, AtomicLong::get)
				.description("Last server-side polling delay returned to clients")
				.tags("eventId", eventId, "state", state)
				.register(meterRegistry);
			return value;
		}).set(pollAfterMs);
	}

	@Override
	public void recordQueueState(String eventId, Long waitingUsers, Long entrySlots) {
		if (waitingUsers != null) {
			registerGauge(waitingUsersByEvent, "ticketon.queue.waiting_users", eventId,
				"Waiting queue user count").set(waitingUsers);
		}
		if (entrySlots != null) {
			registerGauge(entrySlotsByEvent, "ticketon.queue.entry_slots", eventId,
				"Available entry slot count").set(entrySlots);
		}
	}

	private AtomicLong registerGauge(Map<String, AtomicLong> gauges, String name, String eventId,
		String description) {
		return gauges.computeIfAbsent(eventId, key -> {
			AtomicLong value = new AtomicLong();
			Gauge.builder(name, value, AtomicLong::get)
				.description(description)
				.tags(Tags.of("eventId", key))
				.register(meterRegistry);
			return value;
		});
	}
}
