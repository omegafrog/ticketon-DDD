package org.codenbug.auth.domain;

import java.util.List;
import java.util.Optional;

public interface SecurityUserRepository {

	Optional<SecurityUser> findSecurityUser(SecurityUserId securityUserId);
	Optional<SecurityUser> findSecurityUserByEmail(String email);
	List<SecurityUser> findAllSecurityUsers();

	SecurityUserId save(SecurityUser securityUser);

	Optional<SecurityUser> findSecurityUserBySocialId(SocialId socialId);

	void delete(String securityUserId);
}
