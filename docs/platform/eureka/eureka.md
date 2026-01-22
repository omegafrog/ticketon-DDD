# broker server scale out
gateway가 broker server로 라우팅하려고 한다.

broker 서버 도메인이 여러개 이므로, gateway가 broker를 알도록 해야 하는데, 이때 eureka를 사용한다.

`lb://broker` 와 같이 라우팅 uri를 설정하면, gateway가 트래픽 분배 정책에 따라 분배한다.

## Eureka server 작성
1. 새 모듈을 추가하고 의존성 추가
```groovy
// https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-netflix-eureka-server
implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server:4.3.0")
```

2. 메인 애플리케이션 어노테이션 추가
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {
	public static void main(String[] args) {
		SpringApplication.run(EurekaApplication.class, args);
	}
}
```
3. application.yml설정
```groovy
server:
  port: 8761 # Eureka 서버는 관례적으로 8761 포트를 사용합니다.

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false # 자신은 등록하지 않음
    fetch-registry: false      # 레지스트리 정보를 가져오지 않음
```
## Broker를 Eureka 서버에 등록
1. eureka client 의존성 broker에 추가 (다른 모듈도 동일)
```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```
2. application.yml 추가
```groovy
spring:
  application:
    name: broker # Eureka에 등록될 서비스의 고유 이름

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/ # Eureka 서버 주소
  instance:
    instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}

### 현재 서비스 이름 (spring.application.name)
- app
- auth
- broker
- event
- purchase
- seat
- user
```
