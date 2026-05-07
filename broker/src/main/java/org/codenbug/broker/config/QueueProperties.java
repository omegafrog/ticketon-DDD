package org.codenbug.broker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {
  private int maxActiveShoppers = 500;
  private int newUsersPerMinute = 3000;
  private int promotionBatchSize = 50;
  private long promotionIntervalMs = 1000;
  private long entryTokenTtlMinutes = 10;
  private long pollingIntervalSeconds = 5;
  private long maxPollingIntervalSeconds = 15;

  public int getMaxActiveShoppers() {
    return maxActiveShoppers;
  }

  public void setMaxActiveShoppers(int maxActiveShoppers) {
    this.maxActiveShoppers = maxActiveShoppers;
  }

  public int getNewUsersPerMinute() {
    return newUsersPerMinute;
  }

  public void setNewUsersPerMinute(int newUsersPerMinute) {
    this.newUsersPerMinute = newUsersPerMinute;
  }

  public int getPromotionBatchSize() {
    return promotionBatchSize;
  }

  public void setPromotionBatchSize(int promotionBatchSize) {
    this.promotionBatchSize = promotionBatchSize;
  }

  public long getPromotionIntervalMs() {
    return promotionIntervalMs;
  }

  public void setPromotionIntervalMs(long promotionIntervalMs) {
    this.promotionIntervalMs = promotionIntervalMs;
  }

  public long getEntryTokenTtlMinutes() {
    return entryTokenTtlMinutes;
  }

  public void setEntryTokenTtlMinutes(long entryTokenTtlMinutes) {
    this.entryTokenTtlMinutes = entryTokenTtlMinutes;
  }

  public long getPollingIntervalSeconds() {
    return pollingIntervalSeconds;
  }

  public void setPollingIntervalSeconds(long pollingIntervalSeconds) {
    this.pollingIntervalSeconds = pollingIntervalSeconds;
  }

  public long getMaxPollingIntervalSeconds() {
    return maxPollingIntervalSeconds;
  }

  public void setMaxPollingIntervalSeconds(long maxPollingIntervalSeconds) {
    this.maxPollingIntervalSeconds = maxPollingIntervalSeconds;
  }
}
