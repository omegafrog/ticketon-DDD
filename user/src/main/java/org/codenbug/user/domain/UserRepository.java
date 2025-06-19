package org.codenbug.user.domain;

public interface UserRepository {

	UserId save(User user);
	User findUser(UserId id);

	void delete(String userId);
}
