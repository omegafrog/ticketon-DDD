package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.Ticket;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.infra.JpaTicketRepository;
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
@Import(NPlusOnePurchaseRepositoryTest.JpaTestConfig.class)
class NPlusOnePurchaseRepositoryTest {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private JpaTicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessTickets_triggersNPlusOne() {
        int purchaseCount = 3;
        int ticketsPerPurchase = 2;

        for (int i = 0; i < purchaseCount; i++) {
            Purchase purchase = new Purchase("event-1", "order-" + i, 1000, new UserId("user-" + i));
            purchaseRepository.save(purchase);

            for (int j = 0; j < ticketsPerPurchase; j++) {
                Ticket ticket = new Ticket("LOC-" + i + "-" + j, "SEAT-" + i + "-" + j, purchase);
                ticketRepository.save(ticket);
            }
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<Purchase> purchases = purchaseRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (Purchase purchase : purchases) {
            purchase.getTickets().size();
        }

        long totalStatements = statistics.getPrepareStatementCount();
        long extraStatements = totalStatements - statementsAfterFindAll;

        assertThat(extraStatements)
            .as("Expected at least one extra query per purchase when loading tickets lazily")
            .isGreaterThanOrEqualTo(purchaseCount);
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
