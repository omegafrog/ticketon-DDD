package org.codenbug.user.query;

import org.codenbug.auth.domain.QSecurityUser;
import org.codenbug.user.domain.QUser;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.global.dto.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
@Transactional(value = "readOnlyTransactionManager", readOnly = true)
public class UserViewRepositoryImpl implements UserViewRepository {
    
    private final JPAQueryFactory readOnlyQueryFactory;
    private final QUser user = QUser.user;

    public UserViewRepositoryImpl(@Qualifier("readOnlyQueryFactory") JPAQueryFactory readOnlyQueryFactory) {
        this.readOnlyQueryFactory = readOnlyQueryFactory;
    }
    
    @Override
    public User findUserById(UserId userId) {
        return readOnlyQueryFactory
            .select(user)
            .from(user)
            .where(user.userId.eq(userId))
            .fetchOne();
    }
}