package org.codenbug.seat.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SeatClientConfig {

	@Bean("seatRestTemplate")
	@Profile("modulith")
	public RestTemplate modulithRestTemplate(@Qualifier("appRestTemplate") RestTemplate restTemplate) {
		return restTemplate;
	}

	@Bean("seatRestTemplate")
	@Profile("!modulith")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
