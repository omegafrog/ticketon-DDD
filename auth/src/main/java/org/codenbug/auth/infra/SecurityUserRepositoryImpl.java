package org.codenbug.auth.infra;

import java.util.Optional;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.SocialId;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class SecurityUserRepositoryImpl implements SecurityUserRepository {

	private final JpaSecurityUserRepository jpaRepository;

	public SecurityUserRepositoryImpl(JpaSecurityUserRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<SecurityUser> findSecurityUser(SecurityUserId securityUserId) {
		return jpaRepository.findById(securityUserId);
	}

	@Override
	public Optional<SecurityUser> findSecurityUserByEmail(String email) {
		return jpaRepository.findByEmail(email);
	}

	@Override
	public SecurityUserId save(SecurityUser securityUser) {
		return jpaRepository.save(securityUser).getSecurityUserId();
	}

	@Override
	public Optional<SecurityUser> findSecurityUserBySocialId(SocialId socialId) {
		return jpaRepository.findBySocialInfo_SocialId(socialId);
	}

	@Override
	public void delete(String securityUserId) {
		jpaRepository.deleteById(new SecurityUserId(securityUserId));
	}
}
