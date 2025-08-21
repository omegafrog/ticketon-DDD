package org.codenbug.purchase.ui.projection;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구매 리스트 조회용 Projection
 * 뷰 전용으로 필요한 필드만 포함하여 N+1 문제를 방지
 */
public class PurchaseListProjection {
    private final String purchaseId;
    private final String orderId;
    private final String orderName;
    private final String eventId;
    private final int amount;
    private final String paymentMethod;
    private final String paymentStatus;
    private final LocalDateTime createdAt;
    private final String userId;
    private  List<TicketProjection> tickets;
    
    public PurchaseListProjection(String purchaseId, String orderId, String orderName,
                                 String eventId, int amount, String paymentMethod,
                                 String paymentStatus, LocalDateTime createdAt, 
                                 String userId, List<TicketProjection> tickets) {
        this.purchaseId = purchaseId;
        this.orderId = orderId;
        this.orderName = orderName;
        this.eventId = eventId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.userId = userId;
        this.tickets = tickets;
    }
    
    public String getPurchaseId() { return purchaseId; }
    public String getOrderId() { return orderId; }
    public String getOrderName() { return orderName; }
    public String getEventId() { return eventId; }
    public int getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }
    public List<TicketProjection> getTickets() { return tickets; }
    
    /**
     * Tickets 설정을 위한 메서드
     */
    public void setTickets(List<TicketProjection> tickets) {
        this.tickets = tickets;
    }
    
    /**
     * 티켓 정보 Projection
     */
    public static class TicketProjection {
        private final String ticketId;
        private final String location;
        private final String eventId;
        private final String seatId;
        private final String purchaseId; // Purchase ID 추가
        
        public TicketProjection(String ticketId, String location, String eventId, String seatId, String purchaseId) {
            this.ticketId = ticketId;
            this.location = location;
            this.eventId = eventId;
            this.seatId = seatId;
            this.purchaseId = purchaseId;
        }
        
        public String getTicketId() { return ticketId; }
        public String getLocation() { return location; }
        public String getEventId() { return eventId; }
        public String getSeatId() { return seatId; }
        public String getPurchaseId() { return purchaseId; }
    }
}