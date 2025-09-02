package org.codenbug.batch.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "batch.analyze")
public class BatchAnalyzeProperties {
	private List<String> tables;

	public List<String> getTables() {
		return tables;
	}

	public void setTables(List<String> tables) {
		this.tables = tables;
	}
}
