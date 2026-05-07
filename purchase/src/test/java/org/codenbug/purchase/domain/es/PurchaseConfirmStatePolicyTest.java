package org.codenbug.purchase.domain.es;

import static org.assertj.core.api.Assertions.assertThat;

import org.codenbug.purchase.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

class PurchaseConfirmStatePolicyTest {

  @Test
  void terminalPurchaseStatusFor_mapsOnlyTerminalConfirmStatuses() {
    assertThat(PurchaseConfirmStatePolicy.terminalPurchaseStatusFor(PurchaseConfirmStatus.PENDING)).isEmpty();
    assertThat(PurchaseConfirmStatePolicy.terminalPurchaseStatusFor(PurchaseConfirmStatus.PROCESSING)).isEmpty();
    assertThat(PurchaseConfirmStatePolicy.terminalPurchaseStatusFor(PurchaseConfirmStatus.DONE))
        .contains(PaymentStatus.DONE);
    assertThat(PurchaseConfirmStatePolicy.terminalPurchaseStatusFor(PurchaseConfirmStatus.FAILED))
        .contains(PaymentStatus.FAILED);
    assertThat(PurchaseConfirmStatePolicy.terminalPurchaseStatusFor(PurchaseConfirmStatus.REJECTED))
        .contains(PaymentStatus.CANCELED);
  }

  @Test
  void retryableAndTerminalStatusSetsDoNotOverlap() {
    assertThat(PurchaseConfirmStatePolicy.isRetryableConfirmStatus(PurchaseConfirmStatus.PENDING)).isTrue();
    assertThat(PurchaseConfirmStatePolicy.isRetryableConfirmStatus(PurchaseConfirmStatus.PROCESSING)).isTrue();

    assertThat(PurchaseConfirmStatePolicy.isTerminalConfirmStatus(PurchaseConfirmStatus.DONE)).isTrue();
    assertThat(PurchaseConfirmStatePolicy.isTerminalConfirmStatus(PurchaseConfirmStatus.FAILED)).isTrue();
    assertThat(PurchaseConfirmStatePolicy.isTerminalConfirmStatus(PurchaseConfirmStatus.REJECTED)).isTrue();

    assertThat(PurchaseConfirmStatePolicy.isTerminalConfirmStatus(PurchaseConfirmStatus.PENDING)).isFalse();
    assertThat(PurchaseConfirmStatePolicy.isRetryableConfirmStatus(PurchaseConfirmStatus.FAILED)).isFalse();
  }
}
