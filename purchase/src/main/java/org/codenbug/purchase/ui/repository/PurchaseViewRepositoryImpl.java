package org.codenbug.purchase.ui.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.codenbug.purchase.domain.QPurchase;
import org.codenbug.purchase.domain.QTicket;
import org.codenbug.purchase.ui.projection.PurchaseListProjection;
import org.codenbug.purchase.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Transactional(value = "readOnlyTransactionManager", readOnly = true)
public class PurchaseViewRepositoryImpl implements PurchaseViewRepository {
    
    private final JPAQueryFactory readOnlyQueryFactory;
    private final QPurchase purchase = QPurchase.purchase;
    private final QTicket ticket = QTicket.ticket;
    
    public PurchaseViewRepositoryImpl(@Qualifier("readOnlyQueryFactory") JPAQueryFactory readOnlyQueryFactory) {
        this.readOnlyQueryFactory = readOnlyQueryFactory;
    }
    
    @Override
    public Page<PurchaseListProjection> findUserPurchaseList(String userId, List<PaymentStatus> statuses, Pageable pageable) {
        // 1. Purchase 기본 정보 조회
        List<PurchaseListProjection> purchases = readOnlyQueryFactory
            .select(Projections.constructor(PurchaseListProjection.class,
                purchase.purchaseId.value,
                purchase.orderId,
                purchase.orderName,
                purchase.eventId,
                purchase.amount,
                purchase.paymentMethod.stringValue(),
                purchase.paymentStatus.stringValue(),
                purchase.createdAt,
                purchase.userId.value,
                Projections.list(new ArrayList<>()) // 빈 리스트로 초기화
            ))
            .from(purchase)
            .where(purchase.userId.value.eq(userId)
                .and(purchase.paymentStatus.in(statuses)))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(purchase.createdAt.desc())
            .fetch();
        
        // 2. Purchase ID 목록 추출
        List<String> purchaseIds = purchases.stream()
            .map(PurchaseListProjection::getPurchaseId)
            .collect(Collectors.toList());
        
        // 3. Ticket 정보 조회 (한 번의 쿼리로)
        if (!purchaseIds.isEmpty()) {
            List<PurchaseListProjection.TicketProjection> tickets = readOnlyQueryFactory
                .select(Projections.constructor(PurchaseListProjection.TicketProjection.class,
                    ticket.id.value,
                    ticket.location,
                    ticket.purchase.eventId,
                    ticket.seatId,
                    ticket.purchase.purchaseId.value // Purchase ID 포함
                ))
                .from(ticket)
                .where(ticket.purchase.purchaseId.value.in(purchaseIds))
                .fetch();
            
            // 4. Purchase별로 Ticket 그룹핑
            Map<String, List<PurchaseListProjection.TicketProjection>> ticketsByPurchase = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getPurchaseId()));
            
            // 5. Purchase에 Ticket 정보 설정
            purchases.forEach(p -> p.setTickets(ticketsByPurchase.getOrDefault(p.getPurchaseId(), List.of())));
        }
        
        // COUNT 쿼리
        Long total = readOnlyQueryFactory
            .select(purchase.count())
            .from(purchase)
            .where(purchase.userId.value.eq(userId)
                .and(purchase.paymentStatus.in(statuses)))
            .fetchOne();
        
        return new PageImpl<>(purchases, pageable, total != null ? total : 0);
    }
    
    @Override
    public Page<PurchaseListProjection> findEventPurchaseList(String eventId, PaymentStatus paymentStatus, Pageable pageable) {
        // Purchase 기본 정보 조회
        List<PurchaseListProjection> purchases = readOnlyQueryFactory
            .select(Projections.constructor(PurchaseListProjection.class,
                purchase.purchaseId.value,
                purchase.orderId,
                purchase.orderName,
                purchase.eventId,
                purchase.amount,
                purchase.paymentMethod.stringValue(),
                purchase.paymentStatus.stringValue(),
                purchase.createdAt,
                purchase.userId.value,
                Projections.list(new ArrayList<>())
            ))
            .from(purchase)
            .where(purchase.eventId.eq(eventId)
                .and(purchase.paymentStatus.eq(paymentStatus)))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(purchase.createdAt.desc())
            .fetch();
        
        // Ticket 정보 추가 (동일한 로직)
        List<String> purchaseIds = purchases.stream()
            .map(PurchaseListProjection::getPurchaseId)
            .collect(Collectors.toList());
        
        if (!purchaseIds.isEmpty()) {
            List<PurchaseListProjection.TicketProjection> tickets = readOnlyQueryFactory
                .select(Projections.constructor(PurchaseListProjection.TicketProjection.class,
                    ticket.id.value,
                    ticket.location,
                    ticket.purchase.eventId,
                    ticket.seatId,
                    ticket.purchase.purchaseId.value
                ))
                .from(ticket)
                .where(ticket.purchase.purchaseId.value.in(purchaseIds))
                .fetch();
            
            Map<String, List<PurchaseListProjection.TicketProjection>> ticketsByPurchase = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getPurchaseId()));
            
            purchases.forEach(p -> p.setTickets(ticketsByPurchase.getOrDefault(p.getPurchaseId(), List.of())));
        }
        
        Long total = readOnlyQueryFactory
            .select(purchase.count())
            .from(purchase)
            .where(purchase.eventId.eq(eventId)
                .and(purchase.paymentStatus.eq(paymentStatus)))
            .fetchOne();
        
        return new PageImpl<>(purchases, pageable, total != null ? total : 0);
    }
    
    @Override
    public List<PurchaseListProjection> findUserPurchaseListWithCursor(String userId, List<PaymentStatus> statuses, 
                                                                      LocalDateTime cursor, int size) {
        // 커서 기반 조회
        List<PurchaseListProjection> purchases = readOnlyQueryFactory
            .select(Projections.constructor(PurchaseListProjection.class,
                purchase.purchaseId.value,
                purchase.orderId,
                purchase.orderName,
                purchase.eventId,
                purchase.amount,
                purchase.paymentMethod.stringValue(),
                purchase.paymentStatus.stringValue(),
                purchase.createdAt,
                purchase.userId.value,
                Projections.list(new ArrayList<>())
            ))
            .from(purchase)
            .where(purchase.userId.value.eq(userId)
                .and(purchase.paymentStatus.in(statuses))
                .and(cursor != null ? purchase.createdAt.lt(cursor) : null))
            .orderBy(purchase.createdAt.desc())
            .limit(size)
            .fetch();
        
        // Ticket 정보 추가
        List<String> purchaseIds = purchases.stream()
            .map(PurchaseListProjection::getPurchaseId)
            .collect(Collectors.toList());
        
        if (!purchaseIds.isEmpty()) {
            List<PurchaseListProjection.TicketProjection> tickets = readOnlyQueryFactory
                .select(Projections.constructor(PurchaseListProjection.TicketProjection.class,
                    ticket.id.value,
                    ticket.location,
                    ticket.purchase.eventId,
                    ticket.seatId,
                    ticket.purchase.purchaseId.value
                ))
                .from(ticket)
                .where(ticket.purchase.purchaseId.value.in(purchaseIds))
                .fetch();
            
            Map<String, List<PurchaseListProjection.TicketProjection>> ticketsByPurchase = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getPurchaseId()));
            
            purchases.forEach(p -> p.setTickets(ticketsByPurchase.getOrDefault(p.getPurchaseId(), List.of())));
        }
        
        return purchases;
    }
}