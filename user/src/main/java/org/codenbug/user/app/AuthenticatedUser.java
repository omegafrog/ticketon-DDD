package org.codenbug.user.app;

import org.codenbug.common.Role;
import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.codenbug.user.domain.UserId;

public record AuthenticatedUser(String userId, String email, Role role) {

	public static AuthenticatedUser from(UserSecurityToken token) {
		return new AuthenticatedUser(token.getUserId(), token.getEmail(), token.getRole());
	}

	public UserId asUserId() {
		return new UserId(userId);
	}

	public void verifySelf(UserId targetUserId) {
		if (!userId.equals(targetUserId.getValue())) {
			throw new AccessDeniedException("Cannot access other user's information.");
		}
	}

	public void requireRole(Role requiredRole) {
		if (role != requiredRole) {
			throw new AccessDeniedException("Required role is " + requiredRole + ".");
		}
	}
}
