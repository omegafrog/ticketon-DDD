package org.codenbug.purchase.infra.command.es;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseOutboxStoreAdapterTest {
  @Mock
  private JpaPurchaseOutboxRepository repository;

  @Test
  void existsByPurchaseIdAndEventType_usesOutboxIdempotencyKey() {
    PurchaseOutboxStoreAdapter adapter = new PurchaseOutboxStoreAdapter(repository);

    when(repository.existsByPurchaseIdAndEventType("confirm:p1")).thenReturn(true);

    boolean exists = adapter.existsByPurchaseIdAndEventType(new PurchaseId("p1"),
        PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED);

    assertTrue(exists);
    verify(repository).existsByPurchaseIdAndEventType("confirm:p1");
  }
}
