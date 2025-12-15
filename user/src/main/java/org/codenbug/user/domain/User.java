package org.codenbug.user.domain;

import java.time.LocalDateTime;

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

  public User(GenerateUserIdService idSrv, String name, Sex sex, String phoneNum, String location,
      Integer age, SecurityUserId securityUserId) {
    this.userId = idSrv.generateUserId();
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
    this.name = name != null ? name : this.name;
    this.age = age != null ? age : this.age;
    this.location = location != null ? location : this.location;
    this.phoneNum = phoneNum != null ? phoneNum : this.phoneNum;
  }
}
