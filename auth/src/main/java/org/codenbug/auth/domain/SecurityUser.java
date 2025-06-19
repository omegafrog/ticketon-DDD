package org.codenbug.auth.domain;

import static org.codenbug.auth.global.PrivacyConstant.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.uuid.Generators;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class SecurityUser{
	@EmbeddedId
	private SecurityUserId securityUserId;

	@Embedded
	private UserId userId;

	@Embedded
	private SocialInfo socialInfo;

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "password")
	private String password;

	@Column(name = "role", length = 255)
	private String role;

	@Column(name = "created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	@CreatedDate
	private LocalDateTime updatedAt;

	@Column(name = "is_additional_info_completed")
	private Boolean isAdditionalInfoCompleted;

	@Column(name = "account_expired_at")
	private LocalDateTime accountExpiredAt;

	@Column(name = "account_locked")
	private boolean accountLocked;

	@Column(name = "enabled")
	private boolean enabled;

	@Column(name = "last_login_at")
	private Timestamp lastLoginAt;

	@Column(name = "password_expired_at")
	private LocalDateTime passwordExpiredAt;

	@Column(name = "login_attempt_count", nullable = false)
	private Integer loginAttemptCount = 0;

	protected SecurityUser() {
	}

	public SecurityUser( UserId userId, SocialInfo socialInfo, String email,
		String password, String role) {
		LocalDateTime now = LocalDateTime.now();

		socialInfo.validate();

		this.securityUserId = generateSecurityUserId();
		this.userId = userId;
		this.socialInfo = socialInfo;
		this.email = email;
		this.password = password;
		this.role = role;
		this.isAdditionalInfoCompleted = false;
		this.accountExpiredAt = now.plusDays(accountExpiryDays);
		this.accountLocked = false;
		this.enabled = true;
		this.passwordExpiredAt = now.plusDays(passwordExpiryDays);
	}

	private SecurityUserId generateSecurityUserId() {
		return new SecurityUserId(Generators.timeBasedEpochGenerator().generate().toString());
	}

	public void complete(){}

	public void match(String password, PasswordEncoder passwordEncoder) {
		if (!passwordEncoder.matches(password, this.password)){
			throw new AccessDeniedException("Password match failed");
		}
	}
}
