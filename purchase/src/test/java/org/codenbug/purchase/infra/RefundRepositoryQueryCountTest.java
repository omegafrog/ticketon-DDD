package org.codenbug.purchase.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

@DataJpaTest(properties = {
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RefundRepositoryQueryCountTest.TestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class RefundRepositoryQueryCountTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("ticketon")
        .withUsername("ticketon")
        .withPassword("ticketon");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @SpringBootApplication(scanBasePackages = "org.codenbug.purchase")
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.purchase.domain")
    static class TestConfig {

        @Bean
        RefundQueryProbe refundQueryProbe() {
            return new RefundQueryProbe();
        }
    }

    static class RefundQueryProbe {

        @PersistenceContext
        private EntityManager entityManager;

        List<Refund> findByStatusWithoutFetch(RefundStatus status) {
            return entityManager.createQuery(
                    "select r from Refund r where r.status = :status",
                    Refund.class)
                .setParameter("status", status)
                .getResultList();
        }

        Page<Refund> findByUserIdWithoutFetch(UserId userId, PageRequest pageRequest) {
            List<Refund> content = entityManager.createQuery(
                    "select r from Refund r where r.purchase.userId = :userId order by r.requestedAt desc",
                    Refund.class)
                .setParameter("userId", userId)
                .setFirstResult((int) pageRequest.getOffset())
                .setMaxResults(pageRequest.getPageSize())
                .getResultList();

            Long total = entityManager.createQuery(
                    "select count(r) from Refund r where r.purchase.userId = :userId",
                    Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

            return new org.springframework.data.domain.PageImpl<>(content, pageRequest, total);
        }
    }

    private static final UserId TARGET_USER_ID = new UserId("query-count-user");
    private static final UserId OTHER_USER_ID = new UserId("other-user");

    @PersistenceContext
    private EntityManager entityManager;

    @jakarta.annotation.Resource
    private EntityManagerFactory entityManagerFactory;

    @jakarta.annotation.Resource
    private PurchaseRepository purchaseRepository;

    @jakarta.annotation.Resource
    private JpaRefundRepository refundRepository;

    @jakarta.annotation.Resource
    private RefundQueryProbe refundQueryProbe;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        refundRepository.deleteAll();
        purchaseRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        seedRefunds(TARGET_USER_ID, RefundStatus.REQUESTED, 3);
        seedRefunds(OTHER_USER_ID, RefundStatus.REQUESTED, 1);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    @DisplayName("환불 상태 조회에서 현재 DTO가 purchase 식별자만 읽으면 fetch join 전후 모두 1개 쿼리다")
    void 환불_상태_조회_현재_DTO_접근은_N플러스1을_유발하지_않는다() {
        long beforeCount = countPreparedStatements(() -> {
            List<Refund> refunds = refundQueryProbe.findByStatusWithoutFetch(RefundStatus.REQUESTED);
            assertThat(refunds).hasSize(4);
            refunds.forEach(refund -> refund.getPurchase().getPurchaseId().getValue());
        });

        long afterCount = countPreparedStatements(() -> {
            List<Refund> refunds = refundRepository.findByStatus(RefundStatus.REQUESTED);
            assertThat(refunds).hasSize(4);
            refunds.forEach(refund -> refund.getPurchase().getPurchaseId().getValue());
        });

        assertThat(beforeCount).isEqualTo(1L);
        assertThat(afterCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("내 환불 목록 페이지 조회에서 현재 DTO가 purchase 식별자만 읽으면 entity graph 전후 모두 2개 쿼리다")
    void 내_환불_목록_현재_DTO_접근은_N플러스1을_유발하지_않는다() {
        PageRequest pageRequest = PageRequest.of(0, 2);

        long beforeCount = countPreparedStatements(() -> {
            Page<Refund> refunds = refundQueryProbe.findByUserIdWithoutFetch(TARGET_USER_ID, pageRequest);
            assertThat(refunds.getContent()).hasSize(2);
            refunds.getContent().forEach(refund -> refund.getPurchase().getPurchaseId().getValue());
        });

        long afterCount = countPreparedStatements(() -> {
            Page<Refund> refunds = refundRepository.findByPurchaseUserId(TARGET_USER_ID, pageRequest);
            assertThat(refunds.getContent()).hasSize(2);
            refunds.getContent().forEach(refund -> refund.getPurchase().getPurchaseId().getValue());
        });

        assertThat(beforeCount).isEqualTo(2L);
        assertThat(afterCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("환불 상태 조회에서 purchase 일반 필드에 접근하면 join fetch 적용 전 1+N개, 적용 후 1개 쿼리다")
    void 환불_상태_조회에서_purchase_일반_필드_접근시_join_fetch가_N플러스1을_줄인다() {
        long beforeCount = countPreparedStatements(() -> {
            List<Refund> refunds = refundQueryProbe.findByStatusWithoutFetch(RefundStatus.REQUESTED);
            assertThat(refunds).hasSize(4);
            refunds.forEach(refund -> refund.getPurchase().getOrderId());
        });

        long afterCount = countPreparedStatements(() -> {
            List<Refund> refunds = refundRepository.findByStatus(RefundStatus.REQUESTED);
            assertThat(refunds).hasSize(4);
            refunds.forEach(refund -> refund.getPurchase().getOrderId());
        });

        assertThat(beforeCount).isEqualTo(5L);
        assertThat(afterCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("내 환불 목록 페이지 조회에서 purchase 일반 필드에 접근하면 entity graph 적용 전 2+N개, 적용 후 2개 쿼리다")
    void 내_환불_목록에서_purchase_일반_필드_접근시_entity_graph가_N플러스1을_줄인다() {
        PageRequest pageRequest = PageRequest.of(0, 2);

        long beforeCount = countPreparedStatements(() -> {
            Page<Refund> refunds = refundQueryProbe.findByUserIdWithoutFetch(TARGET_USER_ID, pageRequest);
            assertThat(refunds.getContent()).hasSize(2);
            refunds.getContent().forEach(refund -> refund.getPurchase().getOrderId());
        });

        long afterCount = countPreparedStatements(() -> {
            Page<Refund> refunds = refundRepository.findByPurchaseUserId(TARGET_USER_ID, pageRequest);
            assertThat(refunds.getContent()).hasSize(2);
            refunds.getContent().forEach(refund -> refund.getPurchase().getOrderId());
        });

        assertThat(beforeCount).isEqualTo(4L);
        assertThat(afterCount).isEqualTo(2L);
    }

    private long countPreparedStatements(Runnable queryAction) {
        statistics.clear();
        queryAction.run();
        entityManager.flush();
        entityManager.clear();
        return statistics.getPrepareStatementCount();
    }

    private void seedRefunds(UserId userId, RefundStatus status, int count) {
        for (int index = 0; index < count; index++) {
            Purchase purchase = new Purchase(
                "event-" + userId.getValue(),
                "order-" + userId.getValue() + "-" + index,
                1000,
                1L,
                userId
            );
            purchaseRepository.save(purchase);

            Refund refund = Refund.createUserRefund(purchase, 1000, "refund-reason-" + index, userId);
            applyStatus(refund, status);
            refundRepository.save(refund);
        }
    }

    private void applyStatus(Refund refund, RefundStatus status) {
        if (status == RefundStatus.REQUESTED) {
            return;
        }

        if (status == RefundStatus.PROCESSING) {
            refund.startProcessing();
            return;
        }

        if (status == RefundStatus.COMPLETED) {
            refund.startProcessing();
            refund.completeRefund("pg-" + refund.getRefundId().getValue(), "https://receipt.example.com");
            return;
        }

        if (status == RefundStatus.FAILED) {
            refund.startProcessing();
            refund.failRefund("test failure");
            return;
        }

        throw new IllegalArgumentException("Unsupported status for test: " + status);
    }
}
