package org.codenbug.purchase.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.codenbug.message.EventCreatedEvent;
import org.codenbug.purchase.query.model.EventProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * 간단한 Kafka 통합 테스트
 * EventProjectionConsumer의 핵심 동작을 검증합니다.
 */
@DataJpaTest
@EmbeddedKafka(partitions = 1, topics = {"event-created"}, ports = {0})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@DirtiesContext
public class SimpleEventProjectionIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaPurchaseEventProjectionRepository jpaRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testEventCreatedKafkaIntegration() throws InterruptedException {
        // Given: Kafka Producer 설정
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        
        DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Given: Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("simple-test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventCreatedEvent.class);
        
        DefaultKafkaConsumerFactory<String, EventCreatedEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        // Given: EventProjectionConsumer 생성
        EventProjectionConsumer eventProjectionConsumer = new EventProjectionConsumer(jpaRepository);
        CountDownLatch latch = new CountDownLatch(1);

        // Given: 메시지 리스너 컨테이너 설정
        ContainerProperties containerProperties = new ContainerProperties("event-created");
        containerProperties.setMessageListener((MessageListener<String, EventCreatedEvent>) record -> {
            try {
                EventCreatedEvent event = record.value();
                eventProjectionConsumer.handleEventCreated(event);
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("이벤트 처리 실패", e);
            }
        });

        ConcurrentMessageListenerContainer<String, EventCreatedEvent> container = 
            new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
        container.start();

        try {
            // Given: 테스트 이벤트 데이터
            String eventId = "simple-kafka-test-event";
            String title = "Simple Kafka Test Concert";
            String managerId = "simple-manager-123";
            Long seatLayoutId = 999L;
            boolean seatSelectable = true;
            String location = "Simple Test Location";
            String startTime = "2024-12-25T19:00:00";
            String endTime = "2024-12-25T22:00:00";

            EventCreatedEvent event = new EventCreatedEvent(
                eventId, title, managerId, seatLayoutId, seatSelectable, 
                location, startTime, endTime,0, 10000, 1L
            );

            // When: Kafka에 이벤트 발행
            kafkaTemplate.send("event-created", eventId, event);

            // Then: 이벤트가 소비되고 처리될 때까지 대기
            assertTrue(latch.await(10, TimeUnit.SECONDS), 
                "이벤트가 10초 내에 소비되어야 합니다");

            // Then: EventProjection이 데이터베이스에 저장되었는지 확인
            entityManager.clear(); // 영속성 컨텍스트 초기화
            
            EventProjection savedProjection = jpaRepository.findById(eventId).orElse(null);
            assertNotNull(savedProjection, "EventProjection이 저장되어야 합니다");
            
            // Then: 저장된 데이터 검증
            assertAll("Kafka를 통해 저장된 EventProjection 검증",
                () -> assertEquals(eventId, savedProjection.getEventId()),
                () -> assertEquals(title, savedProjection.getTitle()),
                () -> assertEquals(managerId, savedProjection.getManagerId()),
                () -> assertEquals(seatLayoutId, savedProjection.getSeatLayoutId()),
                () -> assertEquals(seatSelectable, savedProjection.isSeatSelectable()),
                () -> assertEquals(location, savedProjection.getLocation()),
                () -> assertEquals(startTime, savedProjection.getStartTime()),
                () -> assertEquals(endTime, savedProjection.getEndTime())
            );

        } finally {
            container.stop();
        }
    }

    @Test
    void testDirectConsumerIntegration() {
        // Given: EventProjectionConsumer와 실제 JPA repository 사용
        EventProjectionConsumer consumer = new EventProjectionConsumer(jpaRepository);
        
        // Given: 테스트 이벤트 데이터
        String eventId = "direct-test-event";
        EventCreatedEvent event = new EventCreatedEvent(
            eventId, "Direct Test Event", "direct-manager", 100L, true,
            "Direct Test Location", "2024-12-25T19:00:00", "2024-12-25T22:00:00",0, 10000, 1L
        );

        // When: 직접 이벤트 처리
        consumer.handleEventCreated(event);
        entityManager.flush();

        // Then: EventProjection 저장 확인
        EventProjection savedProjection = jpaRepository.findById(eventId).orElse(null);
        assertNotNull(savedProjection, "EventProjection이 저장되어야 합니다");
        assertEquals("Direct Test Event", savedProjection.getTitle());
        assertEquals("direct-manager", savedProjection.getManagerId());
    }
}