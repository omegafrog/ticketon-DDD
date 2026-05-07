package org.codenbug.purchase.domain.es;

import java.util.Optional;

import org.codenbug.purchase.domain.PaymentStatus;

public final class PurchaseConfirmStatePolicy {
  private PurchaseConfirmStatePolicy() {
  }

  public static boolean isTerminalConfirmStatus(PurchaseConfirmStatus status) {
    return status == PurchaseConfirmStatus.DONE
        || status == PurchaseConfirmStatus.FAILED
        || status == PurchaseConfirmStatus.REJECTED;
  }

  public static boolean isRetryableConfirmStatus(PurchaseConfirmStatus status) {
    return status == PurchaseConfirmStatus.PENDING
        || status == PurchaseConfirmStatus.PROCESSING;
  }

  public static Optional<PaymentStatus> terminalPurchaseStatusFor(PurchaseConfirmStatus status) {
    return switch (status) {
      case DONE -> Optional.of(PaymentStatus.DONE);
      case FAILED -> Optional.of(PaymentStatus.FAILED);
      case REJECTED -> Optional.of(PaymentStatus.CANCELED);
      case PENDING, PROCESSING -> Optional.empty();
    };
  }
}
