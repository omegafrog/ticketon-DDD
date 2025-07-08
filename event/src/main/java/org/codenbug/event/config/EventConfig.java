package org.codenbug.event.config;

import org.codenbug.categoryid.global.EventCategoryConfig;
import org.codenbug.seat.global.SeatLayoutConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.codenbug.event"})
@EntityScan(basePackages = {"org.codenbug.event"})
@ComponentScan(basePackages = {"org.codenbug.event"})
@Import({SeatLayoutConfig.class,
	EventCategoryConfig.class})
public class EventConfig {
}
