package org.codenbug.seat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SeatClientConfig {

	@Bean("seatRestTemplate")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
