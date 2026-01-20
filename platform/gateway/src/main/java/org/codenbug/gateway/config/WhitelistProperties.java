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
	private List<Urls> urls = new ArrayList<>();

	@Getter
	@Setter
	public static class Urls{
		private String method;
		private String url;
		protected  Urls(){}
		public Urls(String method, String url){
			this.method = method;
			this.url = url;
		}
	}
}


