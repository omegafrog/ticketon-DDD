package org.codenbug.purchase.ui;

import java.util.List;

import org.codenbug.purchase.app.ManagerRefundService;
import org.codenbug.purchase.app.RefundQueryService;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 환불 관련 요청을 처리하는 REST 컨트롤러.
 * 사용자 및 관리자의 환불 내역 조회, 환불 처리 등의 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund", description = "환불 관련 API")
public class RefundController {

	private final RefundQueryService refundQueryService;
	private final ManagerRefundService managerRefundService;

	/**
	 * 현재 로그인한 사용자의 환불 내역을 조회합니다.
	 * 페이지네이션을 지원하여 여러 페이지에 걸쳐 환불 내역을 조회할 수 있습니다.
	 *
	 * @param userId 사용자 ID.
	 * @param pageable 페이지네이션 정보 (페이지 번호, 페이지 크기, 정렬 등).
	 * @return 사용자의 환불 내역 페이지 (RefundDto 목록).
	 */
	@GetMapping("/my")
	@Operation(summary = "내 환불 내역 조회", description = "현재 로그인한 사용자의 환불 내역을 페이지 단위로 조회합니다.")
	public ResponseEntity<Page<RefundQueryService.RefundDto>> getMyRefunds(
		@RequestParam String userId,
		@PageableDefault(size = 10) Pageable pageable) {

		Page<RefundQueryService.RefundDto> refunds = refundQueryService.getUserRefundHistory(userId, pageable);
		return ResponseEntity.ok(refunds);
	}

	/**
	 * 특정 환불의 상세 정보를 조회합니다.
	 * 해당 환불이 특정 사용자에게 속하는지 확인합니다.
	 *
	 * @param refundId 조회할 환불의 고유 ID.
	 * @param userId 환불을 조회하는 사용자의 ID.
	 * @return 특정 환불의 상세 정보 (RefundDto).
	 */
	@GetMapping("/{refundId}")
	@Operation(summary = "특정 환불 상세 조회", description = "특정 환불의 상세 정보를 조회합니다. 해당 환불이 특정 사용자에게 속하는지 확인합니다.")
	public ResponseEntity<RefundQueryService.RefundDto> getRefundDetail(
		@PathVariable String refundId,
		@RequestParam String userId) {

		RefundQueryService.RefundDto refund = refundQueryService.getRefundDetail(refundId, userId);
		return ResponseEntity.ok(refund);
	}

	/**
	 * 관리자가 단일 구매에 대해 환불을 처리합니다.
	 * 환불 사유, 관리자 정보 등을 포함하여 환불을 진행합니다.
	 *
	 * @param request 단일 환불 처리에 필요한 정보를 담은 DTO.
	 * @return 환불 처리 결과 (ManagerRefundResult).
	 */
	@PostMapping("/manager/single")
	@Operation(summary = "매니저 단일 환불 처리", description = "관리자가 단일 구매에 대해 환불을 처리합니다.")
	public ResponseEntity<ManagerRefundService.ManagerRefundResult> processManagerRefund(
		@RequestBody ManagerRefundRequest request) {

		ManagerRefundService.ManagerRefundResult result = managerRefundService.processManagerRefund(
			request.getPurchaseId(),
			request.getRefundReason(),
			request.getManagerName(),
			new UserId(request.getManagerId())
		);

		return ResponseEntity.ok(result);
	}

	/**
	 * 관리자가 특정 이벤트와 관련된 모든 구매에 대해 일괄 환불을 처리합니다.
	 * 주로 이벤트 취소와 같은 상황에서 사용됩니다.
	 *
	 * @param request 일괄 환불 처리에 필요한 정보를 담은 DTO.
	 * @return 각 환불 처리 결과 목록 (ManagerRefundResult 목록).
	 */
	@PostMapping("/manager/batch")
	@Operation(summary = "매니저 일괄 환불 처리", description = "관리자가 특정 이벤트와 관련된 모든 구매에 대해 일괄 환불을 처리합니다. (예: 이벤트 취소)")
	public ResponseEntity<List<ManagerRefundService.ManagerRefundResult>> processBatchRefund(
		@RequestBody BatchRefundRequest request) {

		List<ManagerRefundService.ManagerRefundResult> results = managerRefundService.processBatchRefund(
			request.getEventId(),
			request.getRefundReason(),
			request.getManagerName(),
			new UserId(request.getManagerId())
		);

		return ResponseEntity.ok(results);
	}

	/**
	 * 관리자가 특정 상태(예: PENDING, COMPLETED)의 환불 목록을 조회합니다.
	 * 관리자 대시보드 등에서 환불 현황을 파악하는 데 사용됩니다.
	 *
	 * @param status 조회할 환불 상태.
	 * @return 특정 상태에 해당하는 환불 목록 (RefundDto 목록).
	 */
	@GetMapping("/admin/by-status")
	@Operation(summary = "환불 상태별 조회 (관리자용)", description = "관리자가 특정 상태의 환불 목록을 조회합니다.")
	public ResponseEntity<List<RefundQueryService.RefundDto>> getRefundsByStatus(
		@Parameter(description = "조회할 환불 상태", required = true) @RequestParam RefundStatus status) {

		List<RefundQueryService.RefundDto> refunds = refundQueryService.getRefundsByStatus(status);
		return ResponseEntity.ok(refunds);
	}

	/**
	 * 매니저 환불 요청 DTO
	 * 단일 환불 처리에 필요한 데이터를 담는 내부 클래스입니다.
	 * purchaseId: 환불할 구매 ID
	 * refundReason: 환불 사유
	 * managerId: 환불을 처리하는 관리자 ID
	 * managerName: 환불을 처리하는 관리자 이름
	 */
	public static class ManagerRefundRequest {
		private String purchaseId;
		private String refundReason;
		private String managerId;
		private String managerName;

		// Getters and Setters
		public String getPurchaseId() {
			return purchaseId;
		}

		public void setPurchaseId(String purchaseId) {
			this.purchaseId = purchaseId;
		}

		public String getRefundReason() {
			return refundReason;
		}

		public void setRefundReason(String refundReason) {
			this.refundReason = refundReason;
		}

		public String getManagerId() {
			return managerId;
		}

		public void setManagerId(String managerId) {
			this.managerId = managerId;
		}

		public String getManagerName() {
			return managerName;
		}

		public void setManagerName(String managerName) {
			this.managerName = managerName;
		}
	}

	/**
	 * 일괄 환불 요청 DTO
	 * 일괄 환불 처리에 필요한 데이터를 담는 내부 클래스입니다.
	 * eventId: 환불할 이벤트 ID
	 * refundReason: 환불 사유
	 * managerId: 환불을 처리하는 관리자 ID
	 * managerName: 환불을 처리하는 관리자 이름
	 */
	public static class BatchRefundRequest {
		private String eventId;
		private String refundReason;
		private String managerId;
		private String managerName;

		// Getters and Setters
		public String getEventId() {
			return eventId;
		}

		public void setEventId(String eventId) {
			this.eventId = eventId;
		}

		public String getRefundReason() {
			return refundReason;
		}

		public void setRefundReason(String refundReason) {
			this.refundReason = refundReason;
		}

		public String getManagerId() {
			return managerId;
		}

		public void setManagerId(String managerId) {
			this.managerId = managerId;
		}

		public String getManagerName() {
			return managerName;
		}

		public void setManagerName(String managerName) {
			this.managerName = managerName;
		}
	}
}