package com.jms.spring.config;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class CircuitBreakerAwareMessageListenerContainer extends DefaultMessageListenerContainer
    implements InitializingBean {
  private static final Logger LOG = LoggerFactory.getLogger("CircuitBreakerAwareMessageListenerContainer");

  private RateLimiter rateLimiter;
  private double permitsPerSecond = 10;

  public void setCircuitBreakerFactory(Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  private Resilience4JCircuitBreakerFactory circuitBreakerFactory;

  @Override
  protected boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer) throws JMSException {
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State circuitBreakerState = circuitBreakerFactory.getCircuitBreakerRegistry()
        .circuitBreaker("circuitbreaker").getState();

    LOG.info("***Inside receiveAndExecute, circuit state:{}", circuitBreakerState);

    if (circuitBreakerState.toString().equalsIgnoreCase("OPEN")) {
      LOG.info("Some circuit tripped, hitting rateLimiter...");
      rateLimiter.setRate(0.1);
    } else {
      rateLimiter.setRate(10L);
    }
    rateLimiter.acquire(); // may wait

    return super.receiveAndExecute(invoker, session, consumer);
  }

  public void setRateLimiter(RateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  public void setPermitsPerSecond(double permitsPerSecond) {
    this.permitsPerSecond = permitsPerSecond;
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    this.rateLimiter = RateLimiter.create(permitsPerSecond);
  }
}
