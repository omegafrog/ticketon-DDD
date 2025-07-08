package org.codenbug.categoryid.global;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"org.codenbug.categoryid"})
@ComponentScan(basePackages = {"org.codenbug.categoryid"})
@EnableJpaRepositories(basePackages = {"org.codenbug.categoryid.infra"})
public class EventCategoryConfig {
}
