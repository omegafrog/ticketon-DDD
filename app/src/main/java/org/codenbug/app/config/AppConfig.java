package org.codenbug.app.config;

import org.codenbug.event.config.EventConfig;
import org.codenbug.notification.config.NotificationConfig;
import org.codenbug.purchase.infra.config.PurchaseConfig;
import org.codenbug.securityaop.aop.AopConfig;
import org.codenbug.seat.global.SeatLayoutConfig;
import org.codenbug.user.config.UserConfig;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({
    DatabaseConfig.class,
    UserConfig.class,
    EventConfig.class,
    PurchaseConfig.class,
    SeatLayoutConfig.class,
    NotificationConfig.class,
    AopConfig.class })
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfig {

  @LoadBalanced
  @Bean("appRestTemplate")
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

}
