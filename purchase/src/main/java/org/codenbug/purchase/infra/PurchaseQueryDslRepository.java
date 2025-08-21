package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import static org.codenbug.purchase.domain.QPurchase.purchase;
import static org.codenbug.purchase.domain.QTicket.ticket;

@Repository
@RequiredArgsConstructor
public class PurchaseQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * QueryDSL을 사용하여 Ticket을 드라이빙 테이블로 하는 최적화된 쿼리
     * 기존: Purchase JOIN Ticket (Purchase 드라이빙)
     * 개선: Ticket JOIN Purchase (Ticket 드라이빙) - eventId 조건으로 필터링된 Ticket 수가 적어 성능 향상
     */
    public List<Purchase> findAllByEventIdOptimized(String eventId) {
        return queryFactory
                .selectDistinct(purchase)
                .from(ticket)                           // Ticket을 드라이빙 테이블로 설정
                .join(ticket.purchase, purchase)        // Ticket → Purchase JOIN
                .where(ticket.eventId.value.eq(eventId)) // eventId 조건으로 필터링
                .fetch();
    }
}