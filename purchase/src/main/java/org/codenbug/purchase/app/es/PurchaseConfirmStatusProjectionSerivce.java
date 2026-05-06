package org.codenbug.purchase.app.es;

import java.time.LocalDateTime;
import java.util.Map;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PurchaseConfirmStatusProjectionSerivce {
  private final ObjectMapper objectMapper;
  private final PurchaseConfirmStatusProjectionStore statusProjectionRepository;

  public PurchaseConfirmStatusProjectionSerivce(ObjectMapper objectMapper,
      PurchaseConfirmStatusProjectionStore statusProjectionRepository) {
    this.objectMapper = objectMapper;
    this.statusProjectionRepository = statusProjectionRepository;
  }

  public void upadteProjectionStatus(PurchaseId purchaseId,
      Map<String, Object> payload, PurchaseConfirmStatus status, String message) {
    LocalDateTime now = LocalDateTime.now();

    PurchaseConfirmStatusProjection proj = statusProjectionRepository.findById(purchaseId)
        .orElseGet(() -> PurchaseConfirmStatusProjection.pending(purchaseId, now));
    log.info("Payment confirm status changed to {}.", status);
    proj.update(status, message, now);
    statusProjectionRepository.save(proj);
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("json serialization failed", e);
    }
  }
}
