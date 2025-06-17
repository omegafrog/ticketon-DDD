package org.codenbug.auth.infra;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSecurityUserRepository extends JpaRepository<SecurityUser, SecurityUserId> {
	SecurityUser findByEmail(String email);
}
