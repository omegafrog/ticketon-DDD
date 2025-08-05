package org.codenbug.app.config;

import org.codenbug.event.config.EventConfig;
import org.codenbug.notification.config.NotificationConfig;
import org.codenbug.purchase.config.PurchaseConfig;
import org.codenbug.securityaop.aop.AopConfig;
import org.codenbug.user.config.UserConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@Import({
	UserConfig.class,
	EventConfig.class,
	PurchaseConfig.class,
	NotificationConfig.class,
	AopConfig.class})
@EnableSpringDataWebSupport(
	pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class AppConfig {


}
