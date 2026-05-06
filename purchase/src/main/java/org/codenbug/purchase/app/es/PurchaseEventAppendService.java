package org.codenbug.purchase.app.es;

import java.util.Map;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseEventType;

public interface PurchaseEventAppendService {

  void appendAndUpdateProjection(PurchaseId purchaseId, String commandId, PurchaseEventType eventType,
      Map<String, Object> payload, PurchaseConfirmStatus status, String statusMessage);

  void upadteProjectionStatus(PurchaseId purchaseId, Map<String, Object> payload,
      PurchaseConfirmStatus status, String statusMessage);
}