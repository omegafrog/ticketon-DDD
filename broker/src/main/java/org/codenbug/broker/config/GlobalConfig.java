package org.codenbug.broker.config;

import org.codenbug.securityaop.aop.AopConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Import(
	{AopConfig.class}
)
public class GlobalConfig {
}
