package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.codenbug.user.domain.GenerateUserIdService;
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.infra.JpaUserRepository;
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
@Import(UserNoNPlusOneTest.JpaTestConfig.class)
class UserNoNPlusOneTest {

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessFields_doesNotTriggerExtraQueries() {
        GenerateUserIdService idService = new GenerateUserIdService();

        for (int i = 0; i < 3; i++) {
            User user = new User(
                idService,
                "User" + i,
                Sex.MALE,
                "010-0000-000" + i,
                "Seoul",
                20 + i,
                new SecurityUserId("security-" + i)
            );
            userRepository.save(user);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<User> users = userRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (User user : users) {
            user.getName();
            user.getLocation();
        }

        long totalStatements = statistics.getPrepareStatementCount();

        assertThat(totalStatements)
            .as("Expected no extra queries because User has no relations")
            .isEqualTo(statementsAfterFindAll);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.user.domain")
    @EnableJpaRepositories(basePackages = "org.codenbug.user.infra")
    static class JpaTestConfig {
    }
}
