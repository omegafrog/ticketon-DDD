package org.codenbug.purchase.infra.es;

import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPurchaseOutboxRepository extends JpaRepository<PurchaseOutboxMessage, Long> {
  Optional<PurchaseOutboxMessage> findByMessageId(String messageId);

  @Query("""
      	select m from PurchaseOutboxMessage m
      	where m.publishedAt is null
      	order by  m.id asc
      """)
  List<PurchaseOutboxMessage> findUnpublished(Pageable pageable);

  @Query("""
      	select m from PurchaseOutboxMessage m
      	where m.publishedAt is null
      	  and m.queueName = :queueName
      	order by  m.id asc
      """)
  List<PurchaseOutboxMessage> findUnpublishedByQueueName(String queueName, Pageable pageable);

  @Query("""
          select case
              when count(m) > 0 then true
              else false
          end
          from PurchaseOutboxMessage m
          where m.messageId = :messageId
      """)
  boolean existsByPurchaseIdAndEventType(@Param("messageId") String messageId);
}
