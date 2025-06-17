package org.codenbug.auth.app;

import org.codenbug.auth.domain.AccessToken;
import org.codenbug.auth.domain.Role;
import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.TokenInfo;
import org.codenbug.auth.global.Util;
import org.codenbug.auth.ui.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private SecurityUserRepository securityUserRepository;
	private PasswordEncoder passwordEncoder;
	@Value("${custom.jwt.secret}")
	private String key;

	public AuthService(SecurityUserRepository securityUserRepository, PasswordEncoder passwordEncoder) {
		this.securityUserRepository = securityUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public TokenInfo loginEmail(LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		SecurityUser user = securityUserRepository.findSecurityUserByEmail(email);

		user.match(password, passwordEncoder);

		return Util.generateTokens(user.getUserId(), Role.valueOf(user.getRole()), user.getEmail(),
			Util.Key.convertSecretKey(key));
	}
}
