package org.codenbug.auth.domain;

import static org.codenbug.auth.global.PrivacyConstant.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.codenbug.common.Util;
import org.codenbug.common.Role;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class SecurityUser{
	@EmbeddedId
	private SecurityUserId securityUserId;

	@Column(name = "user_id", unique = true)
	private UserId userId;

	@Embedded
	private SocialInfo socialInfo;

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "password")
	private String password;

	@Column(name = "role", length = 255)
	private String role;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false, length = 32)
	private AccountStatus accountStatus = AccountStatus.ACTIVE;

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

	public SecurityUser( SocialInfo socialInfo, String email,
		String password, String role) {
		LocalDateTime now = LocalDateTime.now();

		socialInfo.validate();

		this.securityUserId = generateSecurityUserId();
		this.socialInfo = socialInfo;
		this.email = email;
		this.password = password;
		this.role = role;
		this.accountStatus = AccountStatus.ACTIVE;
		this.isAdditionalInfoCompleted = false;
		this.accountExpiredAt = now.plusDays(accountExpiryDays);
		this.accountLocked = false;
		this.enabled = true;
		this.passwordExpiredAt = now.plusDays(passwordExpiryDays);
	}

	public static SecurityUser createUserAccount(String email, String encodedPassword) {
		return new SecurityUser(new SocialInfo(null, null, false), email, encodedPassword,
			Role.USER.name());
	}

	public static SecurityUser createSocialUserAccount(SocialId socialId, Provider provider, String email) {
		return new SecurityUser(new SocialInfo(socialId, provider, true), email, null,
			Role.USER.name());
	}

	public static SecurityUser createOperationalAccount(String email, String encodedPassword, Role role) {
		if (role == null) {
			throw new IllegalArgumentException("Account role must not be null.");
		}
		return new SecurityUser(new SocialInfo(null, null, false), email, encodedPassword, role.name());
	}

	private SecurityUserId generateSecurityUserId() {
		return new SecurityUserId(Util.ID.createUUID());
	}

	public void complete(){}

	public void match(String password, PasswordEncoder passwordEncoder) {
		if (!passwordEncoder.matches(password, this.password)){
			throw new AccessDeniedException("Password match failed");
		}
	}

	public void ensureCanAuthenticate() {
		if (!enabled || accountStatus == AccountStatus.SUSPENDED) {
			throw new AccessDeniedException("Suspended account cannot authenticate.");
		}
		if (accountStatus == AccountStatus.DELETED) {
			throw new AccessDeniedException("Deleted account cannot authenticate.");
		}
		if (accountLocked) {
			throw new AccessDeniedException("Locked account cannot authenticate.");
		}
	}

	public void recordAdminLoginFailure() {
		loginAttemptCount = loginAttemptCount == null ? 1 : loginAttemptCount + 1;
		if (loginAttemptCount >= 3) {
			accountLocked = true;
		}
	}

	public void resetLoginFailures() {
		loginAttemptCount = 0;
	}

	public boolean hasRole(Role role) {
		return role != null && role.name().equals(this.role);
	}

	public void promoteToManager() {
		ensureMutable();
		this.role = Role.MANAGER.name();
	}

	public void suspend() {
		ensureNotDeleted();
		this.accountStatus = AccountStatus.SUSPENDED;
		this.enabled = false;
	}

	public void unsuspend() {
		if (accountStatus == AccountStatus.DELETED) {
			throw new IllegalStateException("Deleted account cannot be unsuspended.");
		}
		if (accountStatus != AccountStatus.SUSPENDED) {
			throw new IllegalStateException("Only suspended account can be unsuspended.");
		}
		this.accountStatus = AccountStatus.ACTIVE;
		this.enabled = true;
	}

	public void logicalDelete() {
		this.accountStatus = AccountStatus.DELETED;
		this.enabled = false;
	}

	private void ensureMutable() {
		ensureNotDeleted();
		if (accountStatus == AccountStatus.SUSPENDED) {
			throw new IllegalStateException("Suspended account cannot be changed.");
		}
	}

	private void ensureNotDeleted() {
		if (accountStatus == AccountStatus.DELETED) {
			throw new IllegalStateException("Deleted account cannot be changed.");
		}
	}

	public void updateUserId(UserId userId) {
		this.userId = userId;
	}

	public void linkSocialAccount(SocialId socialId, Provider provider) {
		this.socialInfo = new SocialInfo(socialId, provider, true);
	}

	public String tokenSubject() {
		if (this.userId != null) {
			return this.userId.getValue();
		}
		return this.securityUserId.getValue();
	}
}
