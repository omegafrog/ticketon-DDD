package org.codenbug.broker.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BrokerClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate brokerRestTemplate() {
        return new RestTemplate();
    }
}
