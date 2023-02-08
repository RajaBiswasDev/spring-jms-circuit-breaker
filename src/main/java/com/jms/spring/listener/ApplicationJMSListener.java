package com.jms.spring.listener;

import com.jms.spring.util.ApplicationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApplicationJMSListener {

  private static final Logger LOG = LoggerFactory.getLogger("ApplicationJMSListener");
  private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
  RestTemplate restTemplate = new RestTemplate();

  @Value("${recommendingAppUrl}")
  public String recommendingAppUrl;

  @Autowired
  public ApplicationJMSListener(Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  @JmsListener(destination = ApplicationConstants.MY_QUEUE, containerFactory = "jmsListenerContainerFactory")
  public void handleMessage() {
    LOG.info("Inside handleMessage");

    CircuitBreaker circuitBreaker = circuitBreakerFactory.create(
        "circuitbreaker");
    String processedMessage = circuitBreaker.run(() -> restTemplate.getForObject(recommendingAppUrl, String.class),
        throwable -> fallbackProcessMessage());

    LOG.info("processedMessage:{}", processedMessage);
  }

  public String fallbackProcessMessage() {
    LOG.error("fallback method, circuit state:{}", circuitBreakerFactory.getCircuitBreakerRegistry().
        circuitBreaker("circuitbreaker").getState());
    return "fallback response";
  }
}