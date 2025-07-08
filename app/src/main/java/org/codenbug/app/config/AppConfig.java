package org.codenbug.app.config;

import java.util.Map;

import org.codenbug.event.config.EventConfig;
import org.codenbug.seat.global.SeatLayoutConfig;
import org.codenbug.securityaop.aop.AopConfig;
import org.codenbug.user.config.UserConfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
	UserConfig.class,
	EventConfig.class,

AopConfig.class})
public class AppConfig {


}
