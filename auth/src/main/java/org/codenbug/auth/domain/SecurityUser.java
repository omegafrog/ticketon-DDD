package org.codenbug.auth.domain;

import static org.codenbug.auth.global.PrivacyConstant.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
public class SecurityUser{
	@EmbeddedId
	private SecurityUserId securityUserId;

	@Embedded
	private UserId userId;

	@Embedded
	private SocialInfo socialInfo;

	@Column(name = "email")
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
		this.accountExpiredAt = now.plusDays(accountExpiryDays);
		this.accountLocked = false;
		this.enabled = true;
		this.passwordExpiredAt = now.plusDays(passwordExpiryDays);
	}

	private SecurityUserId generateSecurityUserId() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public void complete(){}


}
