package org.codenbug.auth.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.codenbug.auth.app.AdminAccountView;
import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.domain.AccountStatus;
import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AdminBackofficeControllerTest {
	private MockMvc mockMvc;

	@Mock
	private AuthService authService;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminBackofficeController(authService))
			.setValidator(validator)
			.build();
	}

	@Test
	void 로그인_화면_렌더링() throws Exception {
		mockMvc.perform(get("/admin/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/login"))
			.andExpect(model().attributeExists("loginForm"));
	}

	@Test
	void 로그인_성공_30분_세션_생성_리다이렉트() throws Exception {
		SecurityUser admin = SecurityUser.createOperationalAccount("admin@example.com", "encoded", Role.ADMIN);
		when(authService.authenticateBackofficeAdmin("admin@example.com", "password123")).thenReturn(admin);

		mockMvc.perform(post("/admin/login")
				.param("email", "admin@example.com")
				.param("password", "password123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"))
			.andExpect(request -> {
				MockHttpSession session = (MockHttpSession) request.getRequest().getSession(false);
				org.assertj.core.api.Assertions.assertThat(session).isNotNull();
				org.assertj.core.api.Assertions.assertThat(session.getMaxInactiveInterval())
					.isEqualTo(30 * 60);
			});
	}

	@Test
	void 사용자_목록_세션_없을_시_리다이렉트() throws Exception {
		mockMvc.perform(get("/admin/users"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void 사용자_목록_세션_있으면_마스킹_계정_렌더링() throws Exception {
		MockHttpSession session = adminSession();
		when(authService.findAdminAccountViews()).thenReturn(List.of(
			new AdminAccountView("security-user-1", "ad***in@example.com", "ADMIN",
				AccountStatus.ACTIVE, false, 0)));

		mockMvc.perform(get("/admin/users").session(session))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("createUserForm", "users", "roles"))
			.andExpect(view().name("admin/users"));
	}

	@Test
	void 사용자_생성_세션_필요() throws Exception {
		mockMvc.perform(post("/admin/users")
				.param("email", "user@example.com")
				.param("password", "password123")
				.param("role", "USER"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void 사용자_생성_위임_리다이렉트() throws Exception {
		MockHttpSession session = adminSession();

		mockMvc.perform(post("/admin/users")
				.session(session)
				.param("email", "user@example.com")
				.param("password", "password123")
				.param("role", "USER"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"));

		verify(authService).createAccount("user@example.com", "password123", Role.USER);
	}

	@Test
	void 명령_위임_리다이렉트() throws Exception {
		MockHttpSession session = adminSession();

		mockMvc.perform(post("/admin/users/security-user-2/promote-manager").session(session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"));

		verify(authService).promoteToManager("security-user-2");
	}

	@Test
	void 로그인_유효성_오류_화면_렌더링() throws Exception {
		mockMvc.perform(post("/admin/login").param("email", "not-email").param("password", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/login"));
	}

	private MockHttpSession adminSession() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(AdminBackofficeController.ADMIN_USER_ID_SESSION_KEY, "admin-security-user-1");
		session.setAttribute(AdminBackofficeController.ADMIN_EMAIL_SESSION_KEY, "admin@example.com");
		return session;
	}
}
