package org.codenbug.notification.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.common.Util;
import org.codenbug.notification.application.NotificationCommandService;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.ui.dto.NotificationDto;
import org.codenbug.securityaop.aop.RoleRequiredAspect;
import org.codenbug.securityaop.aop.SecurityAopExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NotificationCommandController.class, TestNotificationController.class})
@Import({NotificationCommandController.class, TestNotificationController.class,
        RoleRequiredAspect.class, SecurityAopExceptionHandler.class,
        NotificationCommandControllerTest.TestExceptionAdvisor.class})
@TestPropertySource(properties = "custom.jwt.secret=01234567890123456789012345678901")
class NotificationCommandControllerTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationCommandService notificationCommandService;

    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        notificationDto = NotificationDto.builder()
                .id(1L)
                .type(NotificationType.SYSTEM)
                .title("제목")
                .content("내용")
                .targetUrl("/target")
                .isRead(false)
                .build();
    }

    @Test
    void ADMIN은_canonical_create에_성공한다() throws Exception {
        when(notificationCommandService.createNotification("user-1", NotificationType.SYSTEM, "제목",
                "내용", "/target")).thenReturn(notificationDto);

        mockMvc.perform(authorizedPost("/api/v1/notifications", Role.ADMIN, validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"));

        verify(notificationCommandService).createNotification("user-1", NotificationType.SYSTEM,
                "제목", "내용", "/target");
    }

    @Test
    void MANAGER는_test_create에도_동일_보안검증_계약으로_성공한다() throws Exception {
        when(notificationCommandService.createNotification("user-1", NotificationType.SYSTEM, "제목",
                "내용", "/target")).thenReturn(notificationDto);

        mockMvc.perform(authorizedPost("/api/v1/test-notifications", Role.MANAGER,
                validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"));

        verify(notificationCommandService).createNotification("user-1", NotificationType.SYSTEM,
                "제목", "내용", "/target");
    }

    @Test
    void USER는_create가_거절된다() throws Exception {
        mockMvc.perform(authorizedPost("/api/v1/notifications", Role.USER, validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));

        verify(notificationCommandService, never()).createNotification(any(), any(), any(), any(),
                any());
    }

    @Test
    void 비인증_요청은_create가_거절된다() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));

        verify(notificationCommandService, never()).createNotification(any(), any(), any(), any(),
                any());
    }

    @Test
    void request_validation_실패는_400이며_service를_호출하지_않는다() throws Exception {
        mockMvc.perform(authorizedPost("/api/v1/notifications", Role.ADMIN, """
                {
                  "userId": "user-1",
                  "type": "SYSTEM",
                  "title": "제목",
                  "content": "   "
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data.content").value("알림 내용은 필수입니다"));

        verify(notificationCommandService, never()).createNotification(any(), any(), any(), any(),
                any());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorizedPost(
            String path, Role role, String body) {
        return post(path)
                .header("Authorization", "Bearer " + createAccessToken("user-1", role,
                        "user-1@ticketon.site"))
                .header("User-Id", "user-1")
                .header("Role", role.name())
                .header("Email", "user-1@ticketon.site")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String validRequestBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "type", "SYSTEM",
                "title", "제목",
                "content", "내용",
                "targetUrl", "/target"));
    }

    private String createAccessToken(String userId, Role role, String email) {
        SecretKey secretKey = Util.Key.convertSecretKey(SECRET);
        return Jwts.builder()
                .claims(Map.of("userId", userId, "role", role.name(), "email", email))
                .expiration(java.util.Date.from(Instant.now().plusSeconds(1800)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @RestControllerAdvice(basePackageClasses = {NotificationCommandController.class,
            TestNotificationController.class})
    static class TestExceptionAdvisor {

        @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
        org.springframework.http.ResponseEntity<RsData<Map<String, Object>>> handleValidation(
                Exception ex) {
            Map<String, Object> errors = new LinkedHashMap<>();

            if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
                methodArgumentNotValidException.getBindingResult().getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            } else if (ex instanceof BindException bindException) {
                bindException.getBindingResult().getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            }

            return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new RsData<>(String.valueOf(HttpStatus.BAD_REQUEST.value()),
                            "요청 값 검증에 실패했습니다.", errors));
        }
    }

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.EnableAspectJAutoProxy
    static class TestApplication {
    }
}
