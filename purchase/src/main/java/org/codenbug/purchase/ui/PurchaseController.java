package org.codenbug.purchase.ui;

import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.common.redis.EntryTokenValidator;
import org.codenbug.purchase.app.PurchaseCancelService;
import org.codenbug.purchase.app.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.es.PurchaseConfirmQueryService;
import org.codenbug.purchase.app.es.PurchaseInitCommandService;
import org.codenbug.purchase.global.CancelPaymentRequest;
import org.codenbug.purchase.global.CancelPaymentResponse;
import org.codenbug.purchase.global.ConfirmPaymentAcceptedResponse;
import org.codenbug.purchase.global.ConfirmPaymentRequest;
import org.codenbug.purchase.global.ConfirmPaymentResponse;
import org.codenbug.purchase.global.ConfirmPaymentStatusResponse;
import org.codenbug.purchase.global.InitiatePaymentRequest;
import org.codenbug.purchase.global.InitiatePaymentResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Purchase", description = "결제 및 티켓 구매 API")
@Validated
public class PurchaseController {
	private final PurchaseCancelService purchaseCancelService;
	private final PurchaseInitCommandService initCommandService;
	private final PurchaseConfirmCommandService confirmCommandService;
	private final PurchaseConfirmQueryService confirmQueryService;
	private final EntryTokenValidator entryTokenValidator;

	@Operation(summary = "결제 준비", description = "티켓 구매를 위한 결제 준비 과정을 시작합니다. 대기열 인증 토큰이 필요합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 준비 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족 또는 잘못된 대기열 토큰"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	@PostMapping("/init")
	@AuthNeeded
	@RoleRequired({Role.USER})
	public ResponseEntity<RsData<InitiatePaymentResponse>> initiatePayment(
		@Parameter(description = "결제 준비 요청 정보", required = true)
		@Valid @RequestBody InitiatePaymentRequest request,
		@Parameter(description = "대기열 진입 인증 토큰", required = true)
		@RequestHeader("entryAuthToken") String entryAuthToken
	) {
		String userId = LoggedInUserContext.get().getUserId();

		entryTokenValidator.validate(userId, entryAuthToken, request.getEventId());
		InitiatePaymentResponse response = initCommandService.initiatePayment(request, userId);
		return ResponseEntity.status(201)
			.body(new RsData<>("201", "결제 준비 완료", response));
	}

	@Operation(summary = "결제 승인", description = "결제를 최종 승인하고 티켓을 발급합니다. Event Sourcing 기반으로 비동기 처리됩니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 승인 요청受理"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족 또는 잘못된 대기열 토큰"),
		@ApiResponse(responseCode = "400", description = "결제 승인 실패")
	})
	@PostMapping("/confirm")
	@AuthNeeded
	@RoleRequired(Role.USER)
	public ResponseEntity<RsData<ConfirmPaymentAcceptedResponse>> confirmPayment(
		@Parameter(description = "결제 승인 요청 정보", required = true)
		@Valid @RequestBody ConfirmPaymentRequest request,
		@Parameter(description = "대기열 진입 인증 토큰", required = true)
		@RequestHeader("entryAuthToken") String entryAuthToken
	) {
		String userId = LoggedInUserContext.get().getUserId();

		ConfirmPaymentAcceptedResponse response = confirmCommandService.requestConfirm(request, userId);
		return ResponseEntity.accepted().body(new RsData<>("202", "결제 승인 요청受理", response));
	}

	@Operation(summary = "결제 승인 상태 조회", description = "Event Sourcing 기반 결제 승인 처리 상태를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "상태 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 부족"),
		@ApiResponse(responseCode = "404", description = "결제 승인 정보를 찾을 수 없음")
	})
	@GetMapping("/confirm/{purchaseId}/status")
	@AuthNeeded
	@RoleRequired(Role.USER)
	public ResponseEntity<RsData<ConfirmPaymentStatusResponse>> getConfirmStatus(
		@Parameter(description = "구매 ID", required = true)
		@PathVariable @NotBlank String purchaseId
	) {
		String userId = LoggedInUserContext.get().getUserId();

		ConfirmPaymentStatusResponse response = confirmQueryService.getStatus(purchaseId, userId);
		return ResponseEntity.ok(new RsData<>("200", "상태 조회 완료", response));
	}

	@Operation(summary = "결제 취소", description = "결제를 취소하고 티켓을 환불 처리합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 취소 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 결제 키 또는 취소 불가능한 상태"),
		@ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
	})
	@PostMapping("/{paymentKey}/cancel")
	public ResponseEntity<RsData<CancelPaymentResponse>> cancelPayment(
		@Parameter(description = "Toss Payments 결제 키", required = true)
		@PathVariable @NotBlank String paymentKey,
		@Parameter(description = "결제 취소 요청 정보", required = true)
		@Valid @RequestBody CancelPaymentRequest request
	) {
		String userId = LoggedInUserContext.get().getUserId();

		CancelPaymentResponse response = purchaseCancelService.cancelPayment(request, paymentKey, userId);
		return ResponseEntity.ok(new RsData<>("200", "결제 취소 완료", response));
	}
}
