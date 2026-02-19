package org.codenbug.gateway.infra;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codenbug.common.RefreshToken;
import org.codenbug.common.exception.BlacklistedRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RedisRefreshTokenBlackListImplTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private RedisRefreshTokenBlackListImpl blackList;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        blackList = new RedisRefreshTokenBlackListImpl(redisTemplate);
    }

    @Test
    void checkBlackList_completesWhenTokenIsNotBlacklisted() {
        RefreshToken refreshToken = new RefreshToken("refresh-token-value");
        when(redisTemplate.hasKey("refreshToken:refresh-token-value")).thenReturn(Mono.just(false));

        StepVerifier.create(blackList.checkBlackList(refreshToken))
            .verifyComplete();

        verify(redisTemplate).hasKey("refreshToken:refresh-token-value");
    }

    @Test
    void checkBlackList_emitsErrorWhenTokenIsBlacklisted() {
        RefreshToken refreshToken = new RefreshToken("blacklisted-token");
        when(redisTemplate.hasKey("refreshToken:blacklisted-token")).thenReturn(Mono.just(true));

        StepVerifier.create(blackList.checkBlackList(refreshToken))
            .expectError(BlacklistedRefreshTokenException.class)
            .verify();

        verify(redisTemplate).hasKey("refreshToken:blacklisted-token");
    }
}
