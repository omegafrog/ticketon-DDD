package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.codenbug.purchase.app.query.es.PurchaseConfirmQueryService;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.ui.response.ConfirmPaymentStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmQueryServiceTest {

  @Mock
  private PurchaseRepository purchaseRepository;
  @Mock
  private PurchaseConfirmStatusProjectionStore projectionRepository;

  @Test
  void getStatus_returnsConfirmStatusAndPurchasePaymentStatusSeparately() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    PurchaseConfirmStatusProjection projection = new PurchaseConfirmStatusProjection(
        purchase.getPurchaseId(),
        PurchaseConfirmStatus.PROCESSING,
        "pg confirm requested",
        LocalDateTime.now());
    PurchaseConfirmQueryService service = new PurchaseConfirmQueryService(purchaseRepository, projectionRepository);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(projectionRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(projection));

    ConfirmPaymentStatusResponse response = service.getStatus(purchase.getPurchaseId().getValue(), "u1");

    assertThat(response.getStatus()).isEqualTo("PROCESSING");
    assertThat(response.getPaymentStatus()).isEqualTo("IN_PROGRESS");
    assertThat(response.getMessage()).isEqualTo("pg confirm requested");
  }
}
