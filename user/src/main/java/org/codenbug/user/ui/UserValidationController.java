package org.codenbug.user.ui;

import org.codenbug.common.RsData;
import org.codenbug.user.app.UserCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserValidationController {
	private final UserCommandService userCommandService;

	@PostMapping("/validate")
	public ResponseEntity<RsData<Void>> validateRegister(@RequestBody RegisterValidationRequest request) {
		userCommandService.validateRegisterInputs(request);
		return ResponseEntity.ok(new RsData<>("200", "사용자 등록 정보 검증 성공", null));
	}
}
