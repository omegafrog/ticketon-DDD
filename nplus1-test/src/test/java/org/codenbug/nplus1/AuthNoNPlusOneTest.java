package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.codenbug.auth.domain.Provider;
import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SocialId;
import org.codenbug.auth.domain.SocialInfo;
import org.codenbug.auth.infra.JpaSecurityUserRepository;
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
@Import(AuthNoNPlusOneTest.JpaTestConfig.class)
class AuthNoNPlusOneTest {

    @Autowired
    private JpaSecurityUserRepository securityUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessFields_doesNotTriggerExtraQueries() {
        for (int i = 0; i < 3; i++) {
            SocialInfo socialInfo = new SocialInfo(new SocialId("social-" + i), new Provider("LOCAL"), true);
            SecurityUser user = new SecurityUser(socialInfo, "user" + i + "@example.com", "pw", "ROLE_USER");
            securityUserRepository.save(user);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<SecurityUser> users = securityUserRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (SecurityUser user : users) {
            user.getEmail();
            user.getRole();
        }

        long totalStatements = statistics.getPrepareStatementCount();

        assertThat(totalStatements)
            .as("Expected no extra queries because SecurityUser has no relations")
            .isEqualTo(statementsAfterFindAll);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.auth.domain")
    @EnableJpaRepositories(basePackages = "org.codenbug.auth.infra")
    static class JpaTestConfig {
    }
}
