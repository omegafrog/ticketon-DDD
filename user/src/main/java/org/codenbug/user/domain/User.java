package org.codenbug.user.domain;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.codenbug.common.Util;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "members")
@Getter
@EntityListeners(AuditingEntityListener.class)
public class User {
  private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01([0|1|6|7|8|9])-\\d{3,4}-\\d{4}$");

  @EmbeddedId
  private UserId userId;

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "sex", nullable = false)
  @Enumerated(EnumType.STRING)
  private Sex sex;

  @Column(name = "phoneNum", length = 255)
  private String phoneNum;

  @Column(name = "addresses", length = 255)
  private String location;

  @Column(name = "age", nullable = false)
  private Integer age;

  @Embedded
  private SecurityUserId securityUserId;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime modifiedAt;

  protected User() {}

  public User(String name, Sex sex, String phoneNum, String location,
      Integer age, SecurityUserId securityUserId) {
    validateName(name);
    validateAge(age);
    validateLocation(location);
    validatePhoneNumber(phoneNum);
    if (sex == null) {
      throw new IllegalArgumentException("Sex must not be null.");
    }
    if (securityUserId == null) {
      throw new IllegalArgumentException("SecurityUserId must not be null.");
    }
    this.userId = generateUserId();
    this.name = name;
    this.sex = sex;
    this.phoneNum = phoneNum;
    this.location = location;
    this.age = age;
    this.securityUserId = securityUserId;
  }

  private UserId generateUserId() {
    return new UserId(Util.ID.createUUID());
  }

  public void update(String name, Integer age, String location, String phoneNum) {
    if (name != null) {
      validateName(name);
    }
    if (age != null) {
      validateAge(age);
    }
    if (location != null) {
      validateLocation(location);
    }
    if (phoneNum != null) {
      validatePhoneNumber(phoneNum);
    }
    this.name = name != null ? name : this.name;
    this.age = age != null ? age : this.age;
    this.location = location != null ? location : this.location;
    this.phoneNum = phoneNum != null ? phoneNum : this.phoneNum;
  }

  private void validateName(String name) {
    if (name == null || name.isBlank() || name.length() > 50) {
      throw new IllegalArgumentException("Name must be 1 to 50 characters.");
    }
  }

  private void validateAge(Integer age) {
    if (age == null || age < 0 || age > 150) {
      throw new IllegalArgumentException("Age must be between 0 and 150.");
    }
  }

  private void validateLocation(String location) {
    if (location == null || location.isBlank() || location.length() > 100) {
      throw new IllegalArgumentException("Location must be 1 to 100 characters.");
    }
  }

  private void validatePhoneNumber(String phoneNum) {
    if (phoneNum == null || !PHONE_NUMBER_PATTERN.matcher(phoneNum).matches()) {
      throw new IllegalArgumentException("Phone number format is invalid.");
    }
  }
}
