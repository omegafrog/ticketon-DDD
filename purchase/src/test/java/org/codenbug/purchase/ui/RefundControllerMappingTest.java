package org.codenbug.purchase.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.codenbug.purchase.ui.command.RefundCommandController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class RefundControllerMappingTest {

	@Test
	void 환불_커맨드_엔드포인트는_커맨드_컨트롤러에서만_소유한다() {
		assertThat(postMappingPaths(RefundCommandController.class))
			.containsExactlyInAnyOrder("/manager/single", "/manager/batch");
		assertThatCodebaseHasNoLegacyRefundController();
	}

	private List<String> postMappingPaths(Class<?> controllerType) {
		return Arrays.stream(controllerType.getDeclaredMethods())
			.map(method -> method.getAnnotation(PostMapping.class))
			.filter(PostMapping.class::isInstance)
			.flatMap(mapping -> Arrays.stream(mapping.value()))
			.toList();
	}

	private void assertThatCodebaseHasNoLegacyRefundController() {
		assertThatThrownByClassLookup("org.codenbug.purchase.ui.RefundController");
	}

	private void assertThatThrownByClassLookup(String className) {
		try {
			Class.forName(className);
		} catch (ClassNotFoundException expected) {
			return;
		}
		throw new AssertionError("Legacy refund controller still exists: " + className);
	}
}
