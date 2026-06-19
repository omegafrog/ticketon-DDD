package org.codenbug.broker.app;

import static org.codenbug.broker.infra.RedisConfig.ENTRY_TOKEN_STORAGE_KEY_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.codenbug.broker.config.QueueProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PollingEntryDispatchServiceTest {

	@Mock
	private EntryAuthService entryAuthService;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("승급 시 입장 토큰을 발급하고 입장 토큰 TTL로 저장한다")
	void 승급_시_입장_토큰_TTL_저장() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(entryAuthService.generateEntryAuthToken(any(), eq("entryAuthToken"))).thenReturn("token");
		QueueProperties queueProperties = new QueueProperties();
		PollingEntryDispatchService service = new PollingEntryDispatchService(entryAuthService, redisTemplate,
			queueProperties, QueueObservation.noop());

		service.handle("user-1", "event-1");

		verify(valueOperations).set(ENTRY_TOKEN_STORAGE_KEY_NAME + ":user-1", "token", 10, TimeUnit.MINUTES);
		verify(valueOperations).set("ENTRY_EVENT:user-1", "event-1", 10, TimeUnit.MINUTES);
	}
}
