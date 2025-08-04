package org.codenbug.app;

import org.codenbug.notification.NotificationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {
    "org.codenbug.app",
    "org.codenbug.user",
    "org.codenbug.event",
    "org.codenbug.purchase",
    "org.codenbug.notification",
    "org.codeNbug.mainserver.global"
})
@EnableJpaAuditing
@Import(NotificationConfig.class)
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
