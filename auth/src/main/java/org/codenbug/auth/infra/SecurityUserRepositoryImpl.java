package org.codenbug.auth.infra;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class SecurityUserRepositoryImpl implements SecurityUserRepository {

	private final JpaSecurityUserRepository jpaRepository;

	public SecurityUserRepositoryImpl(JpaSecurityUserRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public SecurityUser findSecurityUser(SecurityUserId securityUserId) {
		return jpaRepository.findById(securityUserId)
			.orElseThrow(() -> new EntityNotFoundException("Cannot find Security user."));
	}

	@Override
	public SecurityUser findSecurityUserByEmail(String email) {
		return jpaRepository.findByEmail(email);
	}

	@Override
	public SecurityUserId save(SecurityUser securityUser) {
		return jpaRepository.save(securityUser).getSecurityUserId();
	}
}
