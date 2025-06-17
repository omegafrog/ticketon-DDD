package org.codenbug.user.infra;

import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<User, UserId> {
}
