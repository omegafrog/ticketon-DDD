package org.codenbug.purchase.domain.es;

import java.time.LocalDateTime;

import org.codenbug.purchase.domain.PurchaseId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "purchase_confirm_status")
public class PurchaseConfirmStatusProjection {
  @Id
  @Column(name = "purchase_id", length = 64)
  private String purchaseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private PurchaseConfirmStatus status;

  @Column(name = "message", length = 255)
  private String message;

  @Column(name = "last_event_type", length = 64)
  private String lastEventType;

  @Column(name = "last_event_seq")
  private Long lastEventSeq;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected PurchaseConfirmStatusProjection() {
  }

  public PurchaseConfirmStatusProjection(PurchaseId purchaseId, PurchaseConfirmStatus status,
      String message, LocalDateTime updatedAt) {
    this.purchaseId = purchaseId.getValue();
    this.status = status;
    this.message = message;
    this.updatedAt = updatedAt;
  }

  public static PurchaseConfirmStatusProjection pending(PurchaseId purchaseId,
      LocalDateTime now) {
    return new PurchaseConfirmStatusProjection(purchaseId, PurchaseConfirmStatus.PENDING, "accepted", now);
  }

  public static PurchaseConfirmStatusProjection pending(PurchaseId purchaseId, String lastEventType,
      Long lastEventSeq, LocalDateTime now) {
    PurchaseConfirmStatusProjection proj = new PurchaseConfirmStatusProjection(purchaseId, PurchaseConfirmStatus.PENDING, "accepted", now);
    proj.lastEventType = lastEventType;
    proj.lastEventSeq = lastEventSeq;
    return proj;
  }

  public void update(PurchaseConfirmStatus status, Long lastEventSeq, String lastEventType,
      LocalDateTime now) {
    this.status = status;
    this.lastEventSeq = lastEventSeq;
    this.lastEventType = lastEventType;
    this.updatedAt = now;
  }

  public void update(PurchaseConfirmStatus status, String message, Long lastEventSeq, String lastEventType,
      LocalDateTime now) {
    this.status = status;
    this.message = message;
    this.lastEventSeq = lastEventSeq;
    this.lastEventType = lastEventType;
    this.updatedAt = now;
  }

  public void update(PurchaseConfirmStatus status, String message,
      LocalDateTime now) {
    this.status = status;
    this.message = message;
    this.updatedAt = now;
  }

  public void update(PurchaseConfirmStatus status, Long lastEventSeq, String lastEventType,
      String message, LocalDateTime now) {
    this.status = status;
    this.lastEventSeq = lastEventSeq;
    this.lastEventType = lastEventType;
    this.message = message;
    this.updatedAt = now;
  }
}
