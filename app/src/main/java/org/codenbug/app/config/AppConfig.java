package org.codenbug.app.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.codenbug.auth", "org.codenbug.event"})
public class AppConfig {

}
