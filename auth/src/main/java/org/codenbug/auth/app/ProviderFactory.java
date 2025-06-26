package org.codenbug.auth.app;

import java.util.Map;

import org.codenbug.auth.domain.SocialProvider;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProviderFactory {

	private final Map<String, SocialProvider> providerMap;

	public SocialProvider getProvider(String type) {
		return providerMap.get(type.toUpperCase()); // e.g. "KAKAO", "GOOGLE"
	}
}