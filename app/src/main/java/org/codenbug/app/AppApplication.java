package org.codenbug.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(
        scanBasePackages = {"org.codenbug.app", "org.codenbug.user", "org.codenbug.event",
                "org.codenbug.purchase", "org.codenbug.notification", "org.codenbug.redislock"})
@EnableJpaAuditing
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

}
