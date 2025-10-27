package org.codenbug.user.query;

import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;

public interface UserViewRepository {
    User findUserById(UserId userId);
}
