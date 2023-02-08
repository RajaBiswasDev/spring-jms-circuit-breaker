package com.jms.spring.config;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class CircuitBreakerAwareJmsListenerContainerFactory extends DefaultJmsListenerContainerFactory {
  private Resilience4JCircuitBreakerFactory circuitBreakerFactory;

  public void setCircuitBreakerFactory(Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  @Override
  public DefaultMessageListenerContainer createContainerInstance() {
    return new CircuitBreakerAwareMessageListenerContainer();
  }

  @Override
  public DefaultMessageListenerContainer createListenerContainer(JmsListenerEndpoint endpoint) {
    DefaultMessageListenerContainer listenerContainer = super.createListenerContainer(endpoint);

    if (endpoint instanceof MethodJmsListenerEndpoint &&
        listenerContainer instanceof CircuitBreakerAwareMessageListenerContainer) {
      CircuitBreakerAwareMessageListenerContainer container = (CircuitBreakerAwareMessageListenerContainer) listenerContainer;
      container.setCircuitBreakerFactory(circuitBreakerFactory);
    }
    return listenerContainer;
  }
}
