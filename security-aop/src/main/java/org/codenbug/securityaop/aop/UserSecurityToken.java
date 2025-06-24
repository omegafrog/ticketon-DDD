package org.codenbug.securityaop.aop;

import java.util.List;
import java.util.Map;

import org.codenbug.common.Role;

import lombok.Getter;

@Getter
public class UserSecurityToken  {
	private String userId;
	private Role role;
	private String email;

	public Object getPrincipal() {
		return email;
	}

	public Object getCredentials() {
		return null;
	}

	public UserSecurityToken(String userId, String email, Role role) {
		this.email = email;
		this.userId = userId;
		this.role = role;
	}
}
