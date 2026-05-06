package org.codenbug.purchase.app.support;

import org.codenbug.purchase.domain.PaymentProvider;
import org.codenbug.purchase.domain.port.PGApiService;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRouter {
	private final List<PGApiService> providerList;

	public PaymentProviderRouter(List<PGApiService> providerList) {
		this.providerList = providerList;
	}

	public PGApiService get(PaymentProvider targetProvider) {
		for (PGApiService service : providerList) {
			if(service.supports(targetProvider))
				return service;
		}
		throw new IllegalArgumentException("Unsupported payment provider: " + targetProvider);
	}
}
