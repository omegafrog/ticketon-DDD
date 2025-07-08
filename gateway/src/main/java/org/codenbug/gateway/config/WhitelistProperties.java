package org.codenbug.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "filter.whitelist")
@Getter
@Setter
public class WhitelistProperties {
	private List<String> urls = new ArrayList<>();
}


