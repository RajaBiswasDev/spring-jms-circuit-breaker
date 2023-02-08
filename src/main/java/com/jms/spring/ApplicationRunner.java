package com.jms.spring;

import java.time.Duration;
import javax.jms.Queue;

import com.jms.spring.config.CircuitBreakerAwareJmsListenerContainerFactory;
import com.jms.spring.util.ApplicationConstants;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@EnableJms
@SpringBootApplication
public class ApplicationRunner implements CommandLineRunner {
  private static final Logger LOG = LogManager.getLogger();
  private final ApplicationContext appContext;

  public ApplicationRunner(ApplicationContext appContext) {this.appContext = appContext;}

  public static void main(String[] args) {
    SpringApplication springApplication = new SpringApplication(ApplicationRunner.class);
    springApplication.run(args);
  }

  @Override
  public void run(String... strings) {
    JmsTemplate jmsTemplate = appContext.getBean(JmsTemplate.class);

    for (int count = 0; count < 500; count++) {
      LOG.info("*******ApplicationRunner, message sent:{}", count);
      jmsTemplate.convertAndSend(ApplicationConstants.MY_QUEUE, "Message:" + count);
    }
  }

  @Bean
  public Queue queue() {
    return new ActiveMQQueue(ApplicationConstants.MY_QUEUE);
  }

  @Bean
  public JmsTemplate jmsTemplate() {
    JmsTemplate jmsTemplate = new JmsTemplate(queueConnectionFactory());
    jmsTemplate.setDefaultDestinationName(ApplicationConstants.MY_QUEUE);
    return jmsTemplate;
  }

  @Bean
  public ActiveMQConnectionFactory queueConnectionFactory() {
    return new ActiveMQConnectionFactory();
  }

  @Bean
  public CircuitBreakerAwareJmsListenerContainerFactory jmsListenerContainerFactory() {
    CircuitBreakerAwareJmsListenerContainerFactory factory = new CircuitBreakerAwareJmsListenerContainerFactory();
    factory.setConnectionFactory(queueConnectionFactory());
    factory.setCircuitBreakerFactory(appContext.getBean(Resilience4JCircuitBreakerFactory.class));
    return factory;
  }

  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(1)).build())
        .build());
  }
}
