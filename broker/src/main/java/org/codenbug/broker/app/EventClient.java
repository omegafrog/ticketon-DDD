package org.codenbug.broker.app;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface EventClient {
  int getSeatCount(String eventId) throws JsonProcessingException;
}
