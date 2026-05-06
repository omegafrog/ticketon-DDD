package org.codenbug.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("프로필 생성 시 사용자 식별자를 자체 생성하고 개인정보를 보관한다")
    void 프로필_생성시_UserId_자동_생성() {
        User user = new User("User", Sex.MALE, "010-1234-5678", "Seoul", 20,
                new SecurityUserId("security-user-1"));

        assertThat(user.getUserId()).isNotNull();
        assertThat(user.getName()).isEqualTo("User");
        assertThat(user.getAge()).isEqualTo(20);
        assertThat(user.getLocation()).isEqualTo("Seoul");
        assertThat(user.getPhoneNum()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("프로필 수정은 전달된 값만 변경한다")
    void 프로필_수정시_전달된_값만_변경() {
        User user = new User("User", Sex.MALE, "010-1234-5678", "Seoul", 20,
                new SecurityUserId("security-user-1"));

        user.update("Changed", null, "Busan", null);

        assertThat(user.getName()).isEqualTo("Changed");
        assertThat(user.getAge()).isEqualTo(20);
        assertThat(user.getLocation()).isEqualTo("Busan");
        assertThat(user.getPhoneNum()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("프로필 생성 값은 이름, 나이, 성별, 연락처, 지역 형식을 만족해야 한다")
    void 프로필_생성시_값_유효성_검사() {
        assertThatThrownBy(() -> new User("", Sex.MALE, "010-1234-5678", "Seoul", 20,
                new SecurityUserId("security-user-1")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new User("User", Sex.MALE, "invalid", "Seoul", 20,
                new SecurityUserId("security-user-1")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new User("User", Sex.MALE, "010-1234-5678", "Seoul", 151,
                new SecurityUserId("security-user-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("프로필 수정 값도 도메인 형식을 만족해야 한다")
    void 프로필_수정시_값_유효성_검사() {
        User user = new User("User", Sex.MALE, "010-1234-5678", "Seoul", 20,
                new SecurityUserId("security-user-1"));

        assertThatThrownBy(() -> user.update(null, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
