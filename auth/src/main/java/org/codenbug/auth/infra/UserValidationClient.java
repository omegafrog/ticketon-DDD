package org.codenbug.auth.infra;

import org.codenbug.auth.global.UserValidationException;
import org.codenbug.auth.ui.RegisterRequest;
import org.codenbug.common.RsData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserValidationClient {
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final String userServiceBaseUrl;

	public UserValidationClient(RestTemplate restTemplate, ObjectMapper objectMapper,
			@Value("${service.user.base-url}") String userServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.userServiceBaseUrl = userServiceBaseUrl;
	}

	public void validateRegisterInputs(RegisterRequest request) {
		UserRegistrationValidationRequest payload = new UserRegistrationValidationRequest(
				request.getName(),
				request.getAge(),
				request.getSex(),
				request.getPhoneNum(),
				request.getLocation());
		try {
			ResponseEntity<RsData> response = restTemplate.postForEntity(
					userServiceBaseUrl + "/internal/users/validate",
					payload,
					RsData.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new UserValidationException(HttpStatus.BAD_REQUEST,
						new RsData<>("400", "사용자 등록 정보 검증 실패", null));
			}
		} catch (HttpClientErrorException e) {
			throw new UserValidationException(HttpStatus.BAD_REQUEST, parseRsData(e));
		} catch (RestClientException e) {
			log.error("User validation request failed: {}", e.getMessage(), e);
			throw e;
		}
	}

	private RsData<?> parseRsData(HttpClientErrorException e) {
		String body = e.getResponseBodyAsString();
		if (body == null || body.isBlank()) {
			return new RsData<>("400", "사용자 등록 정보 검증 실패", null);
		}

		try {
			return objectMapper.readValue(body, RsData.class);
		} catch (Exception parseException) {
			log.warn("Failed to parse user validation response: {}", parseException.getMessage());
			return new RsData<>("400", "사용자 등록 정보 검증 실패", null);
		}
	}
}
