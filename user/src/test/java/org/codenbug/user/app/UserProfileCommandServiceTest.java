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
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.global.dto.UserInfo;
import org.codenbug.user.ui.UpdateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileCommandServiceTest {

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("USER 역할의 본인 프로필 수정만 허용한다")
	void 본인_USER_역할_프로필_수정_허용() {
		User user = user();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getUserId().getValue(),
			"user@test.local", Role.USER);
		UserProfileCommandService service = new UserProfileCommandService(userRepository);
		when(userRepository.findUser(user.getUserId())).thenReturn(user);

		UserInfo result = service.updateUser(authenticatedUser, user.getUserId(),
			new UpdateUserRequest("Changed", 30, "Busan", "010-9999-8888"));

		assertThat(result.getName()).isEqualTo("Changed");
		assertThat(result.getAge()).isEqualTo(30);
		assertThat(result.getLocation()).isEqualTo("Busan");
		assertThat(result.getPhoneNum()).isEqualTo("010-9999-8888");
		assertThat(result.getRole()).isEqualTo(Role.USER.name());
		verify(userRepository).save(user);
	}

	@Test
	@DisplayName("타인 프로필 수정은 저장소 조회 전에 거절한다")
	void 타인_프로필_수정_거부() {
		User user = user();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser("other-user", "user@test.local",
			Role.USER);
		UserProfileCommandService service = new UserProfileCommandService(userRepository);

		assertThatThrownBy(() -> service.updateUser(authenticatedUser, user.getUserId(),
			new UpdateUserRequest("Changed", 30, "Busan", "010-9999-8888")))
			.isInstanceOf(AccessDeniedException.class);

		verify(userRepository, never()).findUser(user.getUserId());
		verify(userRepository, never()).save(user);
	}

	@Test
	@DisplayName("프로필 수정은 USER 역할이 아니면 거절한다")
	void USER_역할_아닌_프로필_수정_거부() {
		User user = user();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getUserId().getValue(),
			"manager@test.local", Role.MANAGER);
		UserProfileCommandService service = new UserProfileCommandService(userRepository);

		assertThatThrownBy(() -> service.updateUser(authenticatedUser, user.getUserId(),
			new UpdateUserRequest("Changed", 30, "Busan", "010-9999-8888")))
			.isInstanceOf(AccessDeniedException.class);

		verify(userRepository, never()).findUser(user.getUserId());
		verify(userRepository, never()).save(user);
	}

	private User user() {
		return new User("User", Sex.MALE, "010-1234-5678", "Seoul", 20,
			new SecurityUserId("security-user-1"));
	}
}
