package org.codenbug.purchase.global;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PurchaseHistoryListResponse {
    private List<PurchaseSummaryDto> purchases;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PurchaseSummaryDto {
        private String purchaseId;
        private String itemName;
        private Integer amount;
        private LocalDateTime purchaseDate;
        private String paymentMethod;
        private String paymentStatus;
        public static PurchaseSummaryDto of(Purchase purchase){
            return PurchaseHistoryListResponse.PurchaseSummaryDto.builder()
                .purchaseId(purchase.getPurchaseId().getValue())
                .itemName(purchase.getOrderName())
                .amount(purchase.getAmount())
                .purchaseDate(purchase.getCreatedAt())
                .paymentMethod(purchase.getPaymentMethod().name())
                .paymentStatus(purchase.getPaymentStatus().name())
                .build();
        }
    }

    public static PurchaseHistoryListResponse of(Page<PurchaseSummaryDto> page) {
        return PurchaseHistoryListResponse.builder()
                .purchases(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }


} 