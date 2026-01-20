package org.codenbug.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  @Value("${spring.rabbitmq.host}")
  private String host;

  @Value("${spring.rabbitmq.port}")
  private int port;

  @Value("${spring.rabbitmq.username}")
  private String username;

  @Value("${spring.rabbitmq.password}")
  private String password;


  @Bean("sns-user-created")
  public Queue snsUsercreatedQueue() {
    return new Queue("sns-user-created");
  }

  @Bean("security-user-created")
  public Queue securityUserCreatedQueue() {
    return new Queue("security-user-created");
  }

  @Bean
  public DirectExchange topicExchange() {
    return new DirectExchange("user-securityuser-exchanger");
  }

  @Bean
  public Binding userQueueBinding(@Qualifier("security-user-created") Queue queue,
      DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("security-user.created");
  }

  @Bean
  public Binding snsUserQueueBinding(@Qualifier("sns-user-created") Queue queue,
      DirectExchange directExchange) {
    return BindingBuilder.bind(queue).to(directExchange).with("sns-user.created");
  }

  @Bean
  public CachingConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
    connectionFactory.setHost(host);
    connectionFactory.setPort(port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    return connectionFactory;
  }

  /**
   * RabbitTemplate ConnectionFactory 로 연결 후 실제 작업을 위한 Template
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
    return rabbitTemplate;
  }

  /**
   * 직렬화(메세지를 JSON 으로 변환하는 Message Converter)
   */
  @Bean
  public MessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

}
