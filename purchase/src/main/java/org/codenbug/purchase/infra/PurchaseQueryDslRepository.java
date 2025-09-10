package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import static org.codenbug.purchase.domain.QPurchase.purchase;
import static org.codenbug.purchase.domain.QTicket.ticket;

@Repository
public class PurchaseQueryDslRepository {

    private final JPAQueryFactory queryFactory;

	public PurchaseQueryDslRepository(@Qualifier("primaryQueryFactory") JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	/**
     * QueryDSL을 사용하여 최적화된 쿼리
     * Purchase를 드라이빙 테이블로 하여 eventId로 필터링
     */
    public List<Purchase> findAllByEventIdOptimized(String eventId) {
        return queryFactory
                .selectDistinct(purchase)
                .from(purchase)
                .join(purchase.tickets, ticket)
                .where(purchase.eventId.eq(eventId))
                .fetch();
    }
}