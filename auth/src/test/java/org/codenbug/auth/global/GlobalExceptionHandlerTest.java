package org.codenbug.auth.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.codenbug.common.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResponseStatusException은 일반 500 처리로 덮지 않고 원래 상태 코드를 반환한다")
    void responseStatusExceptionKeepsStatusCode() {
        ResponseEntity<RsData<Void>> result = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported social login type"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("400", result.getBody().getCode());
        assertEquals("Unsupported social login type", result.getBody().getMessage());
        assertNull(result.getBody().getData());
    }
}
