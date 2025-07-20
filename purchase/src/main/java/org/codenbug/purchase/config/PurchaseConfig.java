package org.codenbug.purchase.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@EntityScan(basePackages = {"org.codenbug.purchase.domain", "org.codenbug.purchase.query.model"})
@ComponentScan(basePackages = {"org.codenbug.purchase"})
@EnableJpaRepositories(basePackages = {"org.codenbug.purchase.infra"})
public class PurchaseConfig {

	@Bean
	public RestTemplate restTemplate(){
		return new RestTemplate();
	}


	@Bean("objectRedisTemplate")
	public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		return template;
	}
}
