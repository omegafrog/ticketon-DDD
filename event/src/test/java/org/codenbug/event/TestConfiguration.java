package org.codenbug.event;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = {"org.codenbug.event.domain"})
@EnableJpaRepositories(basePackages = {"org.codenbug.event.infra"})
@ComponentScan(basePackages = {"org.codenbug.event"})
public class TestConfiguration {
}