package org.codenbug.purchase.ui.command;

import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.ui.request.BatchRefundRequest;
import org.codenbug.purchase.ui.request.ManagerRefundRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.RsData;
import org.codenbug.purchase.app.command.ManagerRefundService;
import org.codenbug.purchase.domain.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund Command", description = "환불 변경 API")
@Validated
public class RefundCommandController {

	private final ManagerRefundService managerRefundService;

	@PostMapping("/manager/single")
	@Operation(summary = "매니저 단일 환불 처리", description = "관리자가 단일 구매에 대해 환불을 처리합니다.")
	public ResponseEntity<RsData<ManagerRefundService.ManagerRefundResult>> processManagerRefund(
			@Valid @RequestBody ManagerRefundRequest request) {
		ManagerRefundService.ManagerRefundResult result = managerRefundService.processManagerRefund(
			request.getPurchaseId(),
			request.getRefundReason(),
			request.getManagerName(),
			new UserId(request.getManagerId())
		);
		return ResponseEntity.accepted().body(new RsData<>("202", "매니저 단일 환불 처리 요청 완료", result));
	}

	@PostMapping("/manager/batch")
	@Operation(summary = "매니저 일괄 환불 처리", description = "관리자가 특정 이벤트와 관련된 모든 구매에 대해 일괄 환불을 처리합니다. (예: 이벤트 취소)")
	public ResponseEntity<RsData<List<ManagerRefundService.ManagerRefundResult>>> processBatchRefund(
			@Valid @RequestBody BatchRefundRequest request) {
		List<ManagerRefundService.ManagerRefundResult> results = managerRefundService.processBatchRefund(
			request.getEventId(),
			request.getRefundReason(),
			request.getManagerName(),
			new UserId(request.getManagerId())
		);
		return ResponseEntity.accepted().body(new RsData<>("202", "매니저 일괄 환불 처리 요청 완료", results));
	}
}
