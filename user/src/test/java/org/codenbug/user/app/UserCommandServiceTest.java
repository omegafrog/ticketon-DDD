package org.codenbug.user.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import common.ValidationErrors;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("회원가입 이벤트 수신 시 User/Profile BC가 프로필을 생성한다")
	void 회원가입_프로필_생성() {
		UserId savedUserId = new UserId("user-1");
		when(userRepository.save(any(User.class))).thenReturn(savedUserId);
		UserCommandService service = new UserCommandService(userRepository, null);

		UserId result = service.register(new RegisterRequest(new SecurityUserId("security-user-1"),
			"User", 20, "MALE", "010-1234-5678", "Seoul"));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(result).isEqualTo(savedUserId);
		assertThat(userCaptor.getValue().getSecurityUserId().getValue()).isEqualTo("security-user-1");
	}

	@Test
	@DisplayName("프로필 등록 입력 형식이 유효하지 않으면 저장하지 않는다")
	void 회원가입_잘못된_프로필_입력_거부() {
		UserCommandService service = new UserCommandService(userRepository, null);

		assertThatThrownBy(() -> service.register(new RegisterRequest(new SecurityUserId("security-user-1"),
			"", -1, "UNKNOWN", "invalid", "")))
			.isInstanceOf(ValidationErrors.class);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("Auth/Authz 계정 식별자가 없으면 프로필을 생성하지 않는다")
	void 회원가입_보안_사용자_ID_누락_거부() {
		UserCommandService service = new UserCommandService(userRepository, null);

		assertThatThrownBy(() -> service.register(new RegisterRequest(null,
			"User", 20, "MALE", "010-1234-5678", "Seoul")))
			.isInstanceOf(ValidationErrors.class);

		verify(userRepository, never()).save(any(User.class));
	}
}
