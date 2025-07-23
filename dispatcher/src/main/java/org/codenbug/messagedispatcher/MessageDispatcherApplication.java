package org.codenbug.messagedispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MessageDispatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageDispatcherApplication.class, args);
	}

}
