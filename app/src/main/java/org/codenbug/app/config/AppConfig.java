package org.codenbug.app.config;

import org.codenbug.event.config.EventConfig;
import org.codenbug.purchase.config.PurchaseConfig;
import org.codenbug.securityaop.aop.AopConfig;
import org.codenbug.user.config.UserConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
	UserConfig.class,
	EventConfig.class,
	PurchaseConfig.class,
	AopConfig.class})
public class AppConfig {


}
