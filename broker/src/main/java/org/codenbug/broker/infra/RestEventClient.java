package org.codenbug.broker.infra;

import org.codenbug.broker.app.EventClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RestEventClient implements EventClient {

  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  @Value("${custom.events.url}")
  private String url;

  public RestEventClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.restTemplate = new RestTemplate();
  }

  @Override
  public int getSeatCount(String eventId) {
    ResponseEntity<String> forEntity =
        restTemplate.getForEntity(url + "/api/v1/events/" + eventId, String.class);
    try{
      return objectMapper.readTree(forEntity.getBody()).get("data").get("seatCount").asInt();
    }catch (JsonProcessingException e){
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getSeatStatus(String eventId) {
    ResponseEntity<String> forEntity = restTemplate.getForEntity(url + "/api/v1/events/" + eventId, String.class);
    try{
      return objectMapper.readTree(forEntity.getBody()).get("data").get("status").asText();
    }catch (JsonProcessingException e){
      throw new RuntimeException(e);
    }
  }
}
