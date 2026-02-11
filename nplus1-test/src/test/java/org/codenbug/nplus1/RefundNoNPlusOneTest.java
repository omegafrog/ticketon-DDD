package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.infra.JpaRefundRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@DataJpaTest
@Import(RefundNoNPlusOneTest.JpaTestConfig.class)
class RefundNoNPlusOneTest {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private JpaRefundRepository refundRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findByStatus_fetchesPurchaseWithoutExtraQueries() {
        int refundCount = 3;

        for (int i = 0; i < refundCount; i++) {
            Purchase purchase = new Purchase("event-1", "order-" + i, 1000, 1L, new UserId("user-" + i));
            purchaseRepository.save(purchase);

            Refund refund = Refund.createUserRefund(purchase, 100, "reason-" + i, new UserId("user-" + i));
            refundRepository.save(refund);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<Refund> refunds = refundRepository.findByStatus(RefundStatus.REQUESTED);
        long statementsAfterFind = statistics.getPrepareStatementCount();

        for (Refund refund : refunds) {
            refund.getPurchase().getOrderId();
        }

        long totalStatements = statistics.getPrepareStatementCount();
        long extraStatements = totalStatements - statementsAfterFind;

        assertThat(extraStatements)
            .as("Expected no extra queries when purchase is fetched with refund")
            .isEqualTo(0L);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.purchase.domain")
    @EnableJpaRepositories(basePackages = "org.codenbug.purchase.infra")
    static class JpaTestConfig {
    }
}
