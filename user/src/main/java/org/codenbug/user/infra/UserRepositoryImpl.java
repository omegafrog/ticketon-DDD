package org.codenbug.user.infra;

import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class UserRepositoryImpl implements UserRepository {

	public UserRepositoryImpl(JpaUserRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	private final JpaUserRepository jpaRepository;

	@Override
	public UserId save(User user) {
		return jpaRepository.save(user).getUserId();
	}

	@Override
	public User findUser(UserId id) {
		return jpaRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Cannot find User entity"));
	}

	@Override
	public void delete(String userId) {
		jpaRepository.deleteById(new UserId(userId));
	}
}
