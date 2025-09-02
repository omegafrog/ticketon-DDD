package org.codenbug.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.readonly")
public class ReadOnlyDataSourceProperties {
	private String url;
	private String username;
	private String password;
	private String driverClassName;
}
