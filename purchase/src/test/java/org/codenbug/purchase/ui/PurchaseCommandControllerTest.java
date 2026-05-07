package org.codenbug.purchase.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.codenbug.common.Role;
import org.codenbug.common.redis.EntryTokenValidator;
import org.codenbug.purchase.app.command.PendingReservationService;
import org.codenbug.purchase.app.command.PurchaseCancelService;
import org.codenbug.purchase.app.command.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.command.es.PurchaseInitCommandService;
import org.codenbug.purchase.ui.advice.PurchaseExceptionAdvice;
import org.codenbug.purchase.ui.command.PurchaseCommandController;
import org.codenbug.purchase.ui.request.InitiatePaymentRequest;
import org.codenbug.purchase.ui.response.InitiatePaymentResponse;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class PurchaseCommandControllerTest {
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @AfterEach
  void tearDown() {
    LoggedInUserContext.clear();
  }

  @Test
  void initiatePayment_returnsOrderIdInResponseBody() throws Exception {
    PurchaseInitCommandService initCommandService = mock(PurchaseInitCommandService.class);
    EntryTokenValidator entryTokenValidator = mock(EntryTokenValidator.class);
    PurchaseCommandController controller = new PurchaseCommandController(
        mock(PurchaseCancelService.class),
        initCommandService,
        mock(PurchaseConfirmCommandService.class),
        entryTokenValidator,
        mock(PendingReservationService.class));
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new PurchaseExceptionAdvice())
        .build();
    InitiatePaymentRequest request = new InitiatePaymentRequest("event-1", "order-1", 12000);
    LocalDateTime deadline = LocalDateTime.of(2026, 5, 7, 12, 30);

    LoggedInUserContext.open(new UserSecurityToken("user-1", "user1@example.com", Role.USER));
    when(initCommandService.initiatePayment(any(InitiatePaymentRequest.class), eq("user-1")))
        .thenReturn(InitiatePaymentResponse.initiated("purchase-1", "IN_PROGRESS", deadline, "order-1"));

    mockMvc.perform(post("/api/v1/payments/init")
        .header("entryAuthToken", "entry-token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("201"))
        .andExpect(jsonPath("$.data.purchaseId").value("purchase-1"))
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.orderId").value("order-1"));

    verify(entryTokenValidator).validate("user-1", "entry-token", "event-1");
    verify(initCommandService).initiatePayment(any(InitiatePaymentRequest.class), eq("user-1"));
  }
}
