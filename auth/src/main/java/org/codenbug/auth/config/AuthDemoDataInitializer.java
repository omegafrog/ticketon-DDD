package org.codenbug.auth.config;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "ticketon.demo-data.enabled", havingValue = "true")
public class AuthDemoDataInitializer implements ApplicationRunner {
	private final SecurityUserRepository securityUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final RabbitTemplate rabbitTemplate;
	private final String email;
	private final String password;

	public AuthDemoDataInitializer(
		SecurityUserRepository securityUserRepository,
		PasswordEncoder passwordEncoder,
		RabbitTemplate rabbitTemplate,
		@Value("${ticketon.demo-data.user.email:testuser@ticketon.local}") String email,
		@Value("${ticketon.demo-data.user.password:Ticketon123!}") String password) {
		this.securityUserRepository = securityUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.rabbitTemplate = rabbitTemplate;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		securityUserRepository.findSecurityUserByEmail(email)
			.ifPresentOrElse(this::republishProfileEventIfNeeded, this::createDemoUser);
	}

	private void createDemoUser() {
		SecurityUser user = SecurityUser.createUserAccount(email, passwordEncoder.encode(password));
		var securityUserId = securityUserRepository.save(user);
		publishProfileEvent(securityUserId.getValue());
		log.info("Seeded demo auth account: {}", email);
	}

	private void republishProfileEventIfNeeded(SecurityUser user) {
		if (user.getUserId() == null) {
			publishProfileEvent(user.getSecurityUserId().getValue());
		}
	}

	private void publishProfileEvent(String securityUserId) {
		rabbitTemplate.convertAndSend("user-securityuser-exchanger", "security-user.created",
			new SecurityUserRegisteredEvent(
				securityUserId,
				"TicketOn Demo User",
				29,
				"ETC",
				"010-1234-5678",
				"Seoul"));
	}
}
