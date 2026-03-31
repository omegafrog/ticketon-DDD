package org.codenbug.purchase.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.RsData;
import org.codenbug.purchase.app.RefundQueryService;
import org.codenbug.purchase.domain.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund Query", description = "환불 조회 API")
@Validated
public class RefundQueryController {

	private final RefundQueryService refundQueryService;

	@GetMapping("/my")
	@Operation(summary = "내 환불 내역 조회", description = "현재 로그인한 사용자의 환불 내역을 페이지 단위로 조회합니다.")
	public ResponseEntity<RsData<Page<RefundQueryService.RefundDto>>> getMyRefunds(
			@RequestParam @NotBlank String userId,
			@PageableDefault(size = 10) Pageable pageable) {
		Page<RefundQueryService.RefundDto> refunds = refundQueryService.getUserRefundHistory(userId, pageable);
		return ResponseEntity.ok(new RsData<>("200", "내 환불 내역 조회 성공", refunds));
	}

	@GetMapping("/{refundId}")
	@Operation(summary = "특정 환불 상세 조회", description = "특정 환불의 상세 정보를 조회합니다. 해당 환불이 특정 사용자에게 속하는지 확인합니다.")
	public ResponseEntity<RsData<RefundQueryService.RefundDto>> getRefundDetail(
			@PathVariable @NotBlank String refundId,
			@RequestParam @NotBlank String userId) {
		RefundQueryService.RefundDto refund = refundQueryService.getRefundDetail(refundId, userId);
		return ResponseEntity.ok(new RsData<>("200", "특정 환불 상세 조회 성공", refund));
	}

	@GetMapping("/admin/by-status")
	@Operation(summary = "환불 상태별 조회 (관리자용)", description = "관리자가 특정 상태의 환불 목록을 조회합니다.")
	public ResponseEntity<RsData<List<RefundQueryService.RefundDto>>> getRefundsByStatus(
			@Parameter(description = "조회할 환불 상태", required = true)
			@RequestParam RefundStatus status) {
		List<RefundQueryService.RefundDto> refunds = refundQueryService.getRefundsByStatus(status);
		return ResponseEntity.ok(new RsData<>("200", "환불 상태별 조회 성공", refunds));
	}
}
