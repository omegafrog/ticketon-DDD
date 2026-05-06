package org.codenbug.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MaskedUserIdentityTest {
	@Test
	void 이메일_로컬_처음_2개_마스킹_도메인_이름_처음_2개() {
		MaskedUserIdentity identity = MaskedUserIdentity.of("manager@example.com", "홍길동");

		assertThat(identity.maskedEmail()).isEqualTo("ma***er@example.com");
		assertThat(identity.maskedName()).isEqualTo("홍길");
	}

	@Test
	void 짧은_이메일_짧은_이름_마스킹_로컬_부분_노출_없음() {
		MaskedUserIdentity identity = MaskedUserIdentity.of("ab@example.com", "김");

		assertThat(identity.maskedEmail()).isEqualTo("a***@example.com");
		assertThat(identity.maskedName()).isEqualTo("김");
	}

	@Test
	void 잘못된_입력_거부() {
		assertThatThrownBy(() -> MaskedUserIdentity.of("invalid", "홍길동"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> MaskedUserIdentity.of("user@example.com", " "))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
