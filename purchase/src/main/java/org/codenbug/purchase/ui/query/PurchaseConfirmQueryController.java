package org.codenbug.purchase.ui.query;

import org.codenbug.purchase.domain.Purchase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.purchase.app.query.es.PurchaseConfirmQueryService;
import org.codenbug.purchase.ui.response.ConfirmPaymentStatusResponse;
import org.codenbug.securityaop.aop.AuthNeeded;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Purchase Query", description = "결제/구매 조회 API")
public class PurchaseConfirmQueryController {

	private final PurchaseConfirmQueryService confirmQueryService;

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
			@PathVariable @NotBlank String purchaseId) {
		String userId = LoggedInUserContext.get().getUserId();
		ConfirmPaymentStatusResponse response = confirmQueryService.getStatus(purchaseId, userId);
		return ResponseEntity.ok(new RsData<>("200", "상태 조회 완료", response));
	}
}
