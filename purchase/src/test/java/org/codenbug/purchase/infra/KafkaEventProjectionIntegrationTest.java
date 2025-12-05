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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * kafka event 메시지 생성 이후 consumer 소비, repository save까지 테스트
 */
@DataJpaTest
@EmbeddedKafka(partitions = 1, topics = {"event-created"}, ports = {0})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@DirtiesContext
public class KafkaEventProjectionIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaPurchaseEventProjectionRepository jpaRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testEventCreatedEventProducedAndConsumed() throws InterruptedException {
        // Given: Kafka 설정
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Consumer 설정
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        // 수동으로 EventProjectionConsumer 생성
        EventProjectionConsumer consumer = new EventProjectionConsumer(jpaRepository);
        CountDownLatch latch = new CountDownLatch(1);

        // 테스트용 리스너 컨테이너 생성
        ContainerProperties containerProperties = new ContainerProperties("event-created");
        containerProperties.setMessageListener(
                (org.springframework.kafka.listener.MessageListener<String, Object>) record -> {
                    try {
                        EventCreatedEvent event = (EventCreatedEvent) record.value();
                        consumer.handleEventCreated(event);
                        latch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        ConcurrentMessageListenerContainer<String, Object> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
        container.start();

        try {
            // Given: 이벤트 데이터 준비
            String eventId = "kafka-test-event-123";
            String title = "Kafka 테스트 콘서트";
            String managerId = "kafka-manager-456";
            Long seatLayoutId = 789L;
            boolean seatSelectable = true;
            String location = "Kafka 테스트 장소";
            String startTime = "2024-12-25T19:00:00";
            String endTime = "2024-12-25T22:00:00";

            EventCreatedEvent event = new EventCreatedEvent(eventId, title, managerId, seatLayoutId,
                    seatSelectable, location, startTime, endTime, 0, 10000, 1L);

            // When: Kafka에 이벤트 발행
            kafkaTemplate.send("event-created", eventId, event);

            // Then: 이벤트가 소비되고 처리될 때까지 대기
            assertTrue(latch.await(10, TimeUnit.SECONDS), "이벤트가 10초 내에 소비되어야 합니다");

            // DB에서 EventProjection 확인
            entityManager.flush();
            entityManager.clear();

            EventProjection savedProjection = jpaRepository.findById(eventId).orElse(null);
            assertNotNull(savedProjection, "EventProjection이 저장되어야 합니다");

            assertAll("Kafka를 통해 저장된 EventProjection 검증",
                    () -> assertEquals(eventId, savedProjection.getEventId()),
                    () -> assertEquals(title, savedProjection.getTitle()),
                    () -> assertEquals(managerId, savedProjection.getManagerId()),
                    () -> assertEquals(seatLayoutId, savedProjection.getSeatLayoutId()),
                    () -> assertEquals(seatSelectable, savedProjection.isSeatSelectable()),
                    () -> assertEquals(location, savedProjection.getLocation()),
                    () -> assertEquals(startTime, savedProjection.getStartTime()),
                    () -> assertEquals(endTime, savedProjection.getEndTime()));

        } finally {
            container.stop();
        }
    }
}
