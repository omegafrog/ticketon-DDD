package org.codenbug.user.infra.consumer;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.message.UserRegisteredFailedEvent;
import org.codenbug.user.app.UserCommandService;
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecurityUserRegisteredConsumer {

  private final UserCommandService userCommandService;
  private final RabbitTemplate rabbitTemplate;
  private final ApplicationEventPublisher publisher;

  public SecurityUserRegisteredConsumer(UserCommandService userCommandService,
      RabbitTemplate rabbitTemplate, ApplicationEventPublisher publisher) {
    this.userCommandService = userCommandService;
    this.rabbitTemplate = rabbitTemplate;
    this.publisher = publisher;
  }

  @RabbitListener(queues = "security-user-created")
  @Transactional
  public void consume(SecurityUserRegisteredEvent event) {
    try {
      // securityUser로 user를 생성
      SecurityUserId securityUserId = new SecurityUserId(event.getSecurityUserId());
      UserId userId =
          userCommandService.register(new RegisterRequest(securityUserId, event.getName(),
              event.getAge(), event.getSex(), event.getPhoneNum(), event.getLocation()));
      // userId를 securityUser에 추가하도록 이벤트 발행
      publisher.publishEvent(new UserRegisteredEvent(securityUserId.getValue(), userId.getValue()));
    } catch (Exception e) {
      log.error("Failed to create User for securityUserId: {}", event.getSecurityUserId(), e);
      // TODO: securityUser이벤트로 user 생성 실패시 보상 트랜잭션 필요
      rabbitTemplate.convertAndSend("user-securityuser-exchanger", "user.created-failed",
          new UserRegisteredFailedEvent(event.getSecurityUserId()));
    }

  }
}
