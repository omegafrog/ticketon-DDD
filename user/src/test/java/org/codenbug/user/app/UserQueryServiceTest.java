package org.codenbug.user.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codenbug.common.Role;
import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.global.dto.UserInfo;
import org.codenbug.user.query.UserViewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

	@Mock
	private UserViewRepository userViewRepository;

	@Test
	@DisplayName("인증 주체 본인의 프로필만 조회한다")
	void 내_프로필_조회시_본인_프로필_반환() {
		User user = user();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getUserId().getValue(),
			"user@test.local", Role.USER);
		UserQueryService service = new UserQueryService(userViewRepository);
		when(userViewRepository.findUserById(user.getUserId())).thenReturn(user);

		UserInfo result = service.findMe(authenticatedUser, user.getUserId());

		assertThat(result.getUserId()).isEqualTo(user.getUserId().getValue());
		assertThat(result.getEmail()).isEqualTo("user@test.local");
		assertThat(result.getRole()).isEqualTo(Role.USER.name());
	}

	@Test
	@DisplayName("타인 프로필 조회는 거절한다")
	void 내_프로필_조회시_타인_프로필_거부() {
		User user = user();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser("other-user", "user@test.local",
			Role.USER);
		UserQueryService service = new UserQueryService(userViewRepository);

		assertThatThrownBy(() -> service.findMe(authenticatedUser, user.getUserId()))
			.isInstanceOf(AccessDeniedException.class);

		verify(userViewRepository, never()).findUserById(user.getUserId());
	}

	private User user() {
		return new User("User", Sex.MALE, "010-1234-5678", "Seoul", 20,
			new SecurityUserId("security-user-1"));
	}
}
