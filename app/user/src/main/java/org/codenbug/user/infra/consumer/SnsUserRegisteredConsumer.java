package org.codenbug.user.infra.consumer;

import org.codenbug.message.SnsUserRegisteredEvent;
import org.codenbug.message.UserRegisteredEvent;
import org.codenbug.user.app.UserCommandService;
import org.codenbug.user.domain.SecurityUserId;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SnsUserRegisteredConsumer {

  private final RabbitTemplate rabbitTemplate;

  private final UserCommandService userCommandService;

  public SnsUserRegisteredConsumer(UserCommandService userCommandService,
      RabbitTemplate rabbitTemplate) {
    this.userCommandService = userCommandService;
    this.rabbitTemplate = rabbitTemplate;
  }

  @RabbitListener(queues = "sns-user-created")
  @Transactional
  public void consume(SnsUserRegisteredEvent event) {
    try {
      // securityUser로 user를 생성
      UserId userId = userCommandService
          .register(new RegisterRequest(new SecurityUserId(event.getSecurityUserId()),
              event.getName(), event.getAge(), event.getSex(), null, null));
      rabbitTemplate.convertAndSend("user-securityuser-exchanger", "user.created",
          new UserRegisteredEvent(event.getSecurityUserId(), userId.getValue()));
    } catch (Exception e) {
      log.error("Failed to create User for securityUserId: {}", event.getSecurityUserId(), e);
      // TODO: securityUser이벤트로 user 생성 실패시 보상 트랜잭션 필요
    }

  }
}
