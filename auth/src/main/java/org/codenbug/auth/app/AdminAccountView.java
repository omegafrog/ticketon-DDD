package org.codenbug.auth.app;

import org.codenbug.auth.domain.AccountStatus;
import org.codenbug.auth.domain.SecurityUser;

public record AdminAccountView(
	String securityUserId,
	String maskedEmail,
	String role,
	AccountStatus status,
	boolean locked,
	int loginAttemptCount
) {
	public static AdminAccountView from(SecurityUser user) {
		return new AdminAccountView(
			user.getSecurityUserId().getValue(),
			maskEmail(user.getEmail()),
			user.getRole(),
			user.getAccountStatus(),
			user.isAccountLocked(),
			user.getLoginAttemptCount() == null ? 0 : user.getLoginAttemptCount()
		);
	}

	static String maskEmail(String email) {
		if (email == null || email.isBlank() || !email.contains("@")) {
			throw new IllegalArgumentException("Email is invalid.");
		}
		String[] parts = email.split("@", 2);
		String local = parts[0];
		String domain = parts[1];
		if (local.isBlank() || domain.isBlank()) {
			throw new IllegalArgumentException("Email is invalid.");
		}
		if (local.length() <= 4) {
			return local.charAt(0) + "***@" + domain;
		}
		return local.substring(0, 2) + "***" + local.substring(local.length() - 2) + "@" + domain;
	}
}
