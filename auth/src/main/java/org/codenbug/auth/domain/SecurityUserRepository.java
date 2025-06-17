package org.codenbug.auth.domain;

public interface SecurityUserRepository {

	SecurityUser findSecurityUser(SecurityUserId securityUserId);
	SecurityUser findSecurityUserByEmail(String email);

	SecurityUserId save(SecurityUser securityUser);
}
