package org.codenbug.purchase.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.codenbug.common.redis.EntryTokenValidator;
import org.codenbug.common.redis.RedisKeyScanner;
import org.codenbug.purchase.PurchaseTestApplication;
import org.codenbug.purchase.app.PaymentProvider;
import org.codenbug.purchase.app.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.es.PurchaseConfirmScheduler;
import org.codenbug.purchase.app.es.PurchaseConfirmWorker;
import org.codenbug.purchase.app.es.PurchaseInitCommandService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.global.InitiatePaymentRequest;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.TossPaymentPgApiService;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseEventStoreRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.codenbug.redislock.RedisLockService;
import org.codenbug.redislock.RedisLockServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.querydsl.jpa.impl.JPAQueryFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@Testcontainers
@SpringBootTest(classes = PurchaseTestApplication.class)
class PurchaseConfirmWorkerPgMockIntegrationTest {

	@TestConfiguration
	static class JpaAliasConfig {
		@Primary
		@Bean(name = {"primaryEntityManagerFactory", "readOnlyEntityManagerFactory"})
		LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			DataSource dataSource
		) {
			return builder
				.dataSource(dataSource)
				.packages("org.codenbug.purchase.domain")
				.persistenceUnit("purchaseTestPersistenceUnit")
				.build();
		}

		@Primary
		@Bean(name = {"transactionManager", "primaryTransactionManager", "readOnlyTransactionManager"})
		PlatformTransactionManager primaryTransactionManager(
			@Qualifier("primaryEntityManagerFactory") EntityManagerFactory emf
		) {
			return new JpaTransactionManager(emf);
		}

		@Bean(name = "simpleRedisTemplate")
		RedisTemplate<String, String> simpleRedisTemplate(RedisConnectionFactory factory) {
			RedisTemplate<String, String> template = new RedisTemplate<>();
			template.setConnectionFactory(factory);
			template.setKeySerializer(new StringRedisSerializer());
			template.setValueSerializer(new StringRedisSerializer());
			template.afterPropertiesSet();
			return template;
		}

		@Bean
		RedisKeyScanner redisKeyScanner(@Qualifier("simpleRedisTemplate") RedisTemplate<String, String> simpleRedisTemplate) {
			return new RedisKeyScanner(simpleRedisTemplate);
		}

		@Bean
		RedisLockService redisLockService(StringRedisTemplate redisTemplate, RedisKeyScanner redisKeyScanner) {
			return new RedisLockServiceImpl(redisTemplate, redisKeyScanner);
		}

		@Bean
		EntryTokenValidator entryTokenValidator(@Qualifier("objectRedisTemplate") RedisTemplate<String, Object> objectRedisTemplate) {
			return new EntryTokenValidator(objectRedisTemplate);
		}

		@Bean(name = {"primaryQueryFactory", "readOnlyQueryFactory"})
		JPAQueryFactory queryFactory(@Qualifier("primaryEntityManagerFactory") EntityManagerFactory emf) {
			return new JPAQueryFactory(emf.createEntityManager());
		}
	}

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
		.withDatabaseName("ticketon")
		.withUsername("ticketon")
		.withPassword("ticketon");

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
		.withExposedPorts(6379);

	@Container
	static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");

	private static final HttpServer INTERNAL_SERVICE_STUB = createStubServer();
	private static final String INTERNAL_BASE_URL = "http://localhost:" + INTERNAL_SERVICE_STUB.getAddress().getPort();

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

		registry.add("spring.rabbitmq.host", RABBIT::getHost);
		registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);

		registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
		registry.add("spring.task.scheduling.enabled", () -> "false");

		registry.add("services.event.base-url", () -> INTERNAL_BASE_URL);
		registry.add("services.seat.base-url", () -> INTERNAL_BASE_URL);
	}

	@AfterAll
	static void shutdownStubServer() {
		INTERNAL_SERVICE_STUB.stop(0);
	}

	@Autowired
	private PurchaseInitCommandService initCommandService;

	@Autowired
	private PurchaseConfirmCommandService confirmCommandService;

	@Autowired
	private PurchaseConfirmScheduler confirmScheduler;

	@Autowired
	private JpaPurchaseConfirmStatusProjectionRepository projectionRepository;

	@Autowired
	private JpaPurchaseEventStoreRepository eventStoreRepository;

	@Autowired
	private JpaPurchaseOutboxRepository outboxRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockBean
	private TossPaymentPgApiService tossPaymentPgApiService;

	@BeforeEach
	void setUp() {
		ensureCommandUniqueConstraintRemovedForIntegrationPath();

		when(tossPaymentPgApiService.supports(PaymentProvider.TOSS)).thenReturn(true);
		ConfirmedPaymentInfo paymentInfo = new ConfirmedPaymentInfo(
			"payment-key-1",
			"order-1",
			"테스트 주문",
			1000,
			"DONE",
			"CARD",
			"2026-03-26T08:00:00+09:00",
			new ConfirmedPaymentInfo.Receipt("https://receipt.example.com")
		);
		when(tossPaymentPgApiService.confirmPayment(anyString(), anyString(), anyInt(), anyString()))
			.thenReturn(paymentInfo);
	}

	private void ensureCommandUniqueConstraintRemovedForIntegrationPath() {
		Integer indexCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(1) FROM information_schema.statistics " +
				"WHERE table_schema = DATABASE() " +
				"AND table_name = 'purchase_event_store' " +
				"AND index_name = 'uq_purchase_event_store_purchase_command'",
			Integer.class
		);
		if (indexCount != null && indexCount > 0) {
			jdbcTemplate.execute("ALTER TABLE purchase_event_store DROP INDEX uq_purchase_event_store_purchase_command");
		}
	}

	@Test
	void confirmScheduler_processesConfirmToDone_withOnlyPgApiMocked() {
		String userId = "user-1";
		seedSeatLock(userId, "event-1", "A-1");

		var initResponse = initCommandService.initiatePayment(
			new InitiatePaymentRequest("event-1", "order-1", 1000),
			userId
		);

		confirmCommandService.requestConfirm(
			new ConfirmPaymentRequest(initResponse.getPurchaseId(), "payment-key-1", "order-1", 1000, "TOSS"),
			userId
		);

		confirmScheduler.processPendingConfirms();

		Awaitility.await()
			.atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> {
				PurchaseConfirmStatusProjection projection = projectionRepository.findById(initResponse.getPurchaseId())
					.orElseThrow();
				assertThat(projection.getStatus()).isEqualTo(PurchaseConfirmStatus.DONE);
				assertThat(projection.getLastEventType()).isEqualTo(PurchaseEventType.PAYMENT_COMPLETED.name());
			});

		Purchase purchase = purchaseRepository.findById(new PurchaseId(initResponse.getPurchaseId()))
			.orElseThrow();
		assertThat(purchase.isPaymentCompleted()).isTrue();

		List<PurchaseStoredEvent> events = eventStoreRepository.findByPurchaseIdOrderByIdAsc(initResponse.getPurchaseId());
		assertThat(events).extracting(PurchaseStoredEvent::getEventType)
			.contains(PurchaseEventType.CONFIRM_REQUESTED.name())
			.contains(PurchaseEventType.PG_CONFIRM_SUCCEEDED.name())
			.contains(PurchaseEventType.PAYMENT_COMPLETED.name());

		List<PurchaseOutboxMessage> outboxMessages = outboxRepository.findAll();
		assertThat(outboxMessages).hasSize(1);
		assertThat(outboxMessages.get(0).getPublishedAt()).isNotNull();

		verify(tossPaymentPgApiService, times(1))
			.confirmPayment(eq("payment-key-1"), eq("order-1"), eq(1000), eq("confirm:" + initResponse.getPurchaseId()));
	}

	private void seedSeatLock(String userId, String eventId, String seatId) {
		String key = "seat:lock:" + userId + ":" + eventId + ":" + seatId;
		stringRedisTemplate.opsForValue().set(key, "lock-holder");
	}

	private static HttpServer createStubServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
			server.createContext("/", new InternalServiceStubHandler());
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException("failed to start internal stub server", e);
		}
	}

	private static class InternalServiceStubHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			String path = exchange.getRequestURI().getPath();

			if ("GET".equals(method) && path.matches("/internal/events/[^/]+/summary")) {
				writeJson(exchange, 200,
					"{\"eventId\":\"event-1\",\"seatLayoutId\":101,\"seatSelectable\":true,\"status\":\"OPEN\",\"version\":1,\"salesVersion\":1,\"title\":\"테스트 이벤트\"}");
				return;
			}

			if ("POST".equals(method) && path.matches("/internal/events/[^/]+/payment-holds")) {
				writeJson(exchange, 200,
					"{\"holdToken\":\"hold-token-1\",\"expiresAt\":\"2030-01-01T00:00:00\",\"salesVersion\":1}");
				return;
			}

			if ("POST".equals(method) && path.matches("/internal/events/[^/]+/payment-holds/[^/]+/(consume|release)")) {
				writeJson(exchange, 200, "{}");
				return;
			}

			if ("GET".equals(method) && path.matches("/internal/seat-layouts/\\d+")) {
				writeJson(exchange, 200,
					"{\"id\":101,\"seatLayout\":\"layout\",\"hallName\":\"hall\",\"locationName\":\"location\",\"seats\":[{\"id\":\"A-1\",\"signature\":\"A-1\",\"grade\":\"R\",\"price\":1000,\"available\":true}]}");
				return;
			}

			writeJson(exchange, 404, "{\"message\":\"not found\"}");
		}

		private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}
}
