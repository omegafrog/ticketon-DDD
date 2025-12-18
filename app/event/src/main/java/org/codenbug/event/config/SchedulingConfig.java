package org.codenbug.event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 * ViewCount 동기화 배치 스케줄러 활성화
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // @EnableScheduling 애노테이션으로 스케줄링 기능 활성화
    // ViewCountSyncScheduler의 @Scheduled 메서드가 동작함
}