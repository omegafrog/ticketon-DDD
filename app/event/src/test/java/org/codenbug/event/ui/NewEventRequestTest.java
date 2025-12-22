package org.codenbug.event.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.codenbug.common.exception.ControllerParameterValidationFailedException;
import org.codenbug.event.application.dto.request.NewEventRequest;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NewEventRequestTest {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();


    @Test
    @DisplayName("NewEventRequest의 필드는 validation에 성공해야 한다.")
    void createTest() {
        // NewEventRequest의 validation이 실패하도록 테스트 코드를 작성해. NewEventRequest.java의 코드를 참조해.
        // validate() 메소드의 실패로 발생하는 exception을 잡아서 assertion해

        // When & Then
        Assertions
            .assertThatThrownBy(
                () -> new NewEventRequest("title", null, "description", "restriction",
                    "thumbnailUrl", LocalDateTime.now().plusDays(2), // startDate
                    LocalDateTime.now().plusDays(1), // endDate (invalid: before startDate)
                    new RegisterSeatLayoutDto(List.of(), List.of(), "layout", "hallName"),
                    LocalDateTime.now().plusDays(3), // bookingStart
                    LocalDateTime.now().plusDays(1), // bookingEnd
                    0, true))
            .isInstanceOfSatisfying(ControllerParameterValidationFailedException.class, ex -> {
                assertTrue(ex.getFieldErrors().stream()
                    .anyMatch(error -> error.getFieldName().equals("startDate")));
                assertTrue(ex.getFieldErrors().stream()
                    .anyMatch(error -> error.getFieldName().equals("bookingStart")));

            }).hasMessageContaining("파라미터 validation 실패했습니다.");
    }

    @Test
    @DisplayName("NewEventRequest의 필드가 validation에 실패할 경우 error를 반환해야 한다.")
    void createTest2() throws Exception {
        // NewEventRequest 객체를 생성자로 생성해. 이때 @Valid를 통해서 exception이 발생할 수 있는 경우의 값을 넣어서 객체를 생성해.
        NewEventRequest newEventRequest = new NewEventRequest("", new EventCategoryId(1L), "",
            "restriction", "thumbnailUrl", LocalDateTime.now().plusDays(1), // startDate
            LocalDateTime.now().plusDays(2), // endDate (invalid: before startDate)
            new RegisterSeatLayoutDto(List.of(), List.of(), "layout", "hallName"),
            LocalDateTime.now().plusDays(1), // bookingStart
            LocalDateTime.now().plusDays(3), // bookingEnd
            -1, true);

        // When
        Set<ConstraintViolation<NewEventRequest>> violations = validator.validate(newEventRequest);

        // Then
        Assertions.assertThat(violations).isNotEmpty();
        Assertions.assertThat(violations)
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("title"));
        Assertions.assertThat(violations)
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("description"));
        Assertions.assertThat(violations)
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("ageLimit"));
    }
}
