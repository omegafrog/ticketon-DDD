package org.codenbug.purchase.infra.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import org.codenbug.purchase.domain.port.EventInfoProvider;
import org.codenbug.purchase.domain.PaymentValidationService;
import org.codenbug.purchase.domain.PurchaseDomainService;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.port.SeatLayoutProvider;
import org.codenbug.purchase.domain.TicketGenerationService;
import org.codenbug.redislock.RedisLockService;

@Configuration
@EnableJpaRepositories(basePackages = {
    "org.codenbug.purchase.infra" }, entityManagerFactoryRef = "primaryEntityManagerFactory", transactionManagerRef = "primaryTransactionManager")
@EntityScan(basePackages = { "org.codenbug.purchase.domain" })
@ComponentScan(basePackages = { "org.codenbug.purchase" })
public class PurchaseConfig {

  @Bean("purchaseRestTemplate")
  public RestTemplate restTemplate(@Qualifier("appRestTemplate") RestTemplate restTemplate) {
    return restTemplate;
  }

  @Bean("purchaseExternalRestTemplate")
  public RestTemplate purchaseRestTemplate() {
    return new RestTemplate();
  }

  @Bean("objectRedisTemplate")
  public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    return template;
  }

  @Bean
  public PaymentValidationService paymentValidationService(EventInfoProvider eventInfoProvider,
      SeatLayoutProvider seatLayoutProvider) {
    return new PaymentValidationService(eventInfoProvider, seatLayoutProvider);
  }

  @Bean
  public TicketGenerationService ticketGenerationService() {
    return new TicketGenerationService();
  }

  @Bean
  public PurchaseDomainService purchaseDomainService(PaymentValidationService paymentValidationService,
      TicketGenerationService ticketGenerationService, RedisLockService redisLockService) {
    return new PurchaseDomainService(paymentValidationService, ticketGenerationService, redisLockService);
  }

  @Bean
  public RefundDomainService refundDomainService() {
    return new RefundDomainService();
  }
}
