package ma.emsi.microservices.ebankservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import ma.emsi.microservices.ebankservice.services.CustomerBusinessException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Builds CircuitBreakerRegistry with demo thresholds as the default config,
 * then eagerly registers instance {@code customerService}.
 */
@Configuration
public class Resilience4jCustomerConfig {

    public static final String CUSTOMER_SERVICE_CB = "customerService";

    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig demoConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(CustomerBusinessException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(demoConfig);
        registry.addConfiguration(CUSTOMER_SERVICE_CB, demoConfig);
        registry.circuitBreaker(CUSTOMER_SERVICE_CB, CUSTOMER_SERVICE_CB);
        return registry;
    }

    @Bean
    public CircuitBreaker customerServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(CUSTOMER_SERVICE_CB, CUSTOMER_SERVICE_CB);
    }

    @Configuration
    static class CircuitBreakerBootProbe {
        private final CircuitBreakerRegistry circuitBreakerRegistry;

        CircuitBreakerBootProbe(CircuitBreakerRegistry circuitBreakerRegistry) {
            this.circuitBreakerRegistry = circuitBreakerRegistry;
        }

        @PostConstruct
        void assertDemoThresholds() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CUSTOMER_SERVICE_CB);
            int window = cb.getCircuitBreakerConfig().getSlidingWindowSize();
            if (window != 5) {
                throw new IllegalStateException(
                        "customerService CircuitBreaker slidingWindowSize expected 5 but was " + window);
            }
        }
    }
}
