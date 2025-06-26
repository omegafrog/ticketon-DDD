package org.codenbug.auth.global;

import org.codenbug.auth.domain.Provider;
import org.codenbug.auth.domain.SocialId;
import org.codenbug.common.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfo {
	private SocialId socialId;
	private String name;
	private Provider provider;
	private String email;
	private Role role;

	public UserInfo(String socialId, String name, String provider, String email, String role){
		this.socialId = new SocialId(socialId);
		this.name = name;
		this.provider = new Provider(provider);
		this.email = email;
		this.role = Role.valueOf(role);
	}
}
