package org.codenbug.auth.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.global.UserInfo;
import org.codenbug.auth.ui.SocialLoginResponse;
import org.codenbug.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private SecurityUserRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ProviderFactory providerFactory;

    @Mock
    private SocialProvider socialProvider;

    private OAuthService oauthService;

    @BeforeEach
    void setUp() {
        oauthService = new OAuthService(repository, publisher, new ObjectMapper(), providerFactory);
        ReflectionTestUtils.setField(oauthService, "jwtSecret",
                "testSecretKeyForJwtTokenGenerationThatIsLongEnough");
    }

    @Test
    @DisplayName("소셜 로그인 최초 가입 기준은 이메일이며 동일 이메일 계정에 소셜 정보를 연동한다")
    void 소셜_로그인_이메일_연동_기존_계정_연결() {
        SecurityUser existingUser = SecurityUser.createUserAccount("same@example.com",
                "encoded-password");
        UserInfo userInfo = new UserInfo("social-1", "Same User", "kakao", "same@example.com",
                Role.USER.name(), 20, "M");

        when(providerFactory.getProvider("KAKAO")).thenReturn(socialProvider);
        when(socialProvider.requestAccessToken("code")).thenReturn("{\"access_token\":\"access-token\"}");
        when(socialProvider.getUserInfo(SocialLoginType.KAKAO, "access-token")).thenReturn("{}");
        when(socialProvider.parseUserInfo("{}", SocialLoginType.KAKAO)).thenReturn(userInfo);
        when(repository.findSecurityUserByEmail("same@example.com")).thenReturn(Optional.of(existingUser));
        when(repository.save(any(SecurityUser.class))).thenReturn(new SecurityUserId("security-user-1"));

        SocialLoginResponse response =
                oauthService.requestAccessTokenAndSaveUser(SocialLoginType.KAKAO, "code");

        assertNotNull(response.tokenInfo());
        ArgumentCaptor<SecurityUser> captor = ArgumentCaptor.forClass(SecurityUser.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getSocialInfo().getSocialId());
        verify(publisher, org.mockito.Mockito.never()).publishEvent(any(Object.class));
    }
}
