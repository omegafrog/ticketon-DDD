package org.codenbug.broker.config;

import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class InstanceConfig {

	private String instanceId;

	@PostConstruct
	public void initializeInstanceId() {
		this.instanceId = UUID.randomUUID().toString();
	}

	public String getInstanceId() {
		return instanceId;
	}
}