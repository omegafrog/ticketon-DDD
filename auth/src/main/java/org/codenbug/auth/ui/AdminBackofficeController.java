package org.codenbug.auth.ui;

import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.common.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AdminBackofficeController {
	static final String ADMIN_USER_ID_SESSION_KEY = "adminSecurityUserId";
	static final String ADMIN_EMAIL_SESSION_KEY = "adminEmail";
	static final int ADMIN_SESSION_TIMEOUT_SECONDS = 30 * 60;

	private final AuthService authService;

	public AdminBackofficeController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/admin/login")
	public String loginForm(Model model) {
		if (!model.containsAttribute("loginForm")) {
			model.addAttribute("loginForm", new AdminLoginForm());
		}
		return "admin/login";
	}

	@PostMapping("/admin/login")
	public String login(
		@Valid @ModelAttribute("loginForm") AdminLoginForm form,
		BindingResult bindingResult,
		HttpSession session,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			return "admin/login";
		}
		try {
			SecurityUser admin = authService.authenticateBackofficeAdmin(form.getEmail(), form.getPassword());
			session.setMaxInactiveInterval(ADMIN_SESSION_TIMEOUT_SECONDS);
			session.setAttribute(ADMIN_USER_ID_SESSION_KEY, admin.getSecurityUserId().getValue());
			session.setAttribute(ADMIN_EMAIL_SESSION_KEY, admin.getEmail());
			return "redirect:/admin/users";
		} catch (AccessDeniedException e) {
			redirectAttributes.addFlashAttribute("error", "사용자 오류");
			return "redirect:/admin/login";
		}
	}

	@PostMapping("/admin/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/admin/login";
	}

	@GetMapping("/admin/users")
	public String users(Model model, HttpSession session) {
		if (!isAdminSession(session)) {
			return "redirect:/admin/login";
		}
		model.addAttribute("adminEmail", session.getAttribute(ADMIN_EMAIL_SESSION_KEY));
		model.addAttribute("users", authService.findAdminAccountViews());
		if (!model.containsAttribute("createUserForm")) {
			model.addAttribute("createUserForm", new AdminCreateUserForm());
		}
		model.addAttribute("roles", Role.values());
		return "admin/users";
	}

	@PostMapping("/admin/users")
	public String createUser(
		@Valid @ModelAttribute("createUserForm") AdminCreateUserForm form,
		BindingResult bindingResult,
		HttpSession session,
		RedirectAttributes redirectAttributes,
		Model model
	) {
		if (!isAdminSession(session)) {
			return "redirect:/admin/login";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("adminEmail", session.getAttribute(ADMIN_EMAIL_SESSION_KEY));
			model.addAttribute("users", authService.findAdminAccountViews());
			model.addAttribute("roles", Role.values());
			return "admin/users";
		}
		try {
			authService.createAccount(form.getEmail(), form.getPassword(), form.getRole());
			log.info("admin account change action=create actor={} targetEmail={} role={}",
				session.getAttribute(ADMIN_USER_ID_SESSION_KEY), form.getEmail(), form.getRole());
			redirectAttributes.addFlashAttribute("message", "계정이 생성되었습니다.");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("error", "사용자 오류");
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/admin/users/{securityUserId}/promote-manager")
	public String promoteToManager(@PathVariable String securityUserId, HttpSession session,
		RedirectAttributes redirectAttributes) {
		return runAdminCommand(session, redirectAttributes, "promote-manager", securityUserId,
			() -> authService.promoteToManager(securityUserId), "매니저로 승급되었습니다.");
	}

	@PostMapping("/admin/users/{securityUserId}/suspend")
	public String suspend(@PathVariable String securityUserId, HttpSession session,
		RedirectAttributes redirectAttributes) {
		return runAdminCommand(session, redirectAttributes, "suspend", securityUserId,
			() -> authService.suspend(securityUserId), "계정이 정지되었습니다.");
	}

	@PostMapping("/admin/users/{securityUserId}/unsuspend")
	public String unsuspend(@PathVariable String securityUserId, HttpSession session,
		RedirectAttributes redirectAttributes) {
		return runAdminCommand(session, redirectAttributes, "unsuspend", securityUserId,
			() -> authService.unsuspend(securityUserId), "계정 정지가 해제되었습니다.");
	}

	@PostMapping("/admin/users/{securityUserId}/delete")
	public String delete(@PathVariable String securityUserId, HttpSession session,
		RedirectAttributes redirectAttributes) {
		if (!isAdminSession(session)) {
			return "redirect:/admin/login";
		}
		String adminId = (String) session.getAttribute(ADMIN_USER_ID_SESSION_KEY);
		return runAdminCommand(session, redirectAttributes, "delete", securityUserId,
			() -> authService.logicalDelete(adminId, securityUserId), "계정이 삭제되었습니다.");
	}

	private String runAdminCommand(HttpSession session, RedirectAttributes redirectAttributes,
		String action, String targetId, Runnable command, String successMessage) {
		if (!isAdminSession(session)) {
			return "redirect:/admin/login";
		}
		try {
			command.run();
			log.info("admin account change action={} actor={} target={}",
				action, session.getAttribute(ADMIN_USER_ID_SESSION_KEY), targetId);
			redirectAttributes.addFlashAttribute("message", successMessage);
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("error", "사용자 오류");
		}
		return "redirect:/admin/users";
	}

	private boolean isAdminSession(HttpSession session) {
		return session != null && session.getAttribute(ADMIN_USER_ID_SESSION_KEY) != null;
	}
}
