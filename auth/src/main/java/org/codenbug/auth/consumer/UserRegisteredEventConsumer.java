package org.codenbug.auth.consumer;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.UserId;
import org.codenbug.message.UserRegisteredEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserRegisteredEventConsumer {
  private final SecurityUserRepository repository;

  public UserRegisteredEventConsumer(SecurityUserRepository repository) {
    this.repository = repository;
  }

  @RabbitListener(queues = "user-created")
  @Transactional
  public void consume(UserRegisteredEvent event) {
    try {
      SecurityUser securityUser =
          repository.findSecurityUser(new SecurityUserId(event.getSecurityUserId()))
              .orElseThrow(() -> new EntityNotFoundException("Cannot find SecurityUser entity"));

      securityUser.updateUserId(new UserId(event.getUserId()));
      log.info("Successfully registered security user for userId: {}", event.getSecurityUserId());
    } catch (Exception e) {
    }
  }

}
