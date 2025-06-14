package org.codenbug.auth.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.Getter;

@Getter
public class UserSecurityToken extends AbstractAuthenticationToken {
	private UserId userId;
	private Role role;

	@Override
	public Object getPrincipal() {
		return userId;
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	protected UserSecurityToken() {
		super(List.of());
	}
	public UserSecurityToken(UserId userId, String email, Role role) {
		super(List.of(new SimpleGrantedAuthority(role.name())));
		this.userId = userId;
		this.role = role;
		setDetails(Map.of("email", email));
		setAuthenticated(true);
	}
}
