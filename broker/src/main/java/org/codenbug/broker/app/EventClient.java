package org.codenbug.broker.app;

public interface EventClient {
  int getSeatCount(String eventId);

  String getSeatStatus(String eventId);
}
