package org.codenbug.auth.infra;

import java.util.List;
import java.util.Optional;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SocialId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSecurityUserRepository extends JpaRepository<SecurityUser, SecurityUserId> {
	Optional<SecurityUser> findByEmail(String email);

	Optional<SecurityUser> findBySocialInfo_SocialId(SocialId socialInfoSocialId);
}
