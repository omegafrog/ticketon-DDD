package org.codenbug.app.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = {"org.codenbug.auth", "org.codenbug.event"})
public class JpaConfig {
}
