package org.codenbug.gateway.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CustomErrorResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(throwable -> {
            // 다운스트림 서비스에서 에러 응답이 온 경우 (WebClientResponseException 발생)
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) throwable;
                ServerHttpResponse response = exchange.getResponse();

                // 원래의 HTTP 상태 코드와 응답 본문을 설정합니다.
                HttpStatusCode statusCode = ex.getStatusCode();
                byte[] responseBody = ex.getResponseBodyAsByteArray();
                String originalBody = new String(responseBody, StandardCharsets.UTF_8);

                response.setStatusCode(statusCode);
                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

                DataBuffer buffer = response.bufferFactory().wrap(responseBody);

                // 응답 본문을 클라이언트에게 전송하고 필터 체인을 종료합니다.
                return response.writeWith(Mono.just(buffer))
                        .doOnError(error -> DataBufferUtils.release(buffer));
            }

            // 그 외 다른 예외는 기본 에러 핸들러에 맡깁니다.
            return Mono.error(throwable);
        });
    }

    @Override
    public int getOrder() {
        // 다른 필터들보다 먼저 실행되도록 순서를 높게 설정합니다.
        // DefaultErrorWebExceptionHandler 보다 먼저 실행되어야 합니다.
        return -1;
    }
}