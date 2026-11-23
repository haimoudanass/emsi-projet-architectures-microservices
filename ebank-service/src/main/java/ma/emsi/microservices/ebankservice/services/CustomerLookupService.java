package ma.emsi.microservices.ebankservice.services;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ma.emsi.microservices.ebankservice.clients.CustomerRestClient;
import ma.emsi.microservices.ebankservice.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Dedicated bean so @CircuitBreaker AOP proxy is applied (no self-invocation).
 * Read path: technical failure → fallback null (never invent a Customer).
 * Create path: same breaker name; fallback always refuses creation.
 */
@Service
public class CustomerLookupService {

    private static final Logger log = LoggerFactory.getLogger(CustomerLookupService.class);

    private final CustomerRestClient customerRestClient;

    public CustomerLookupService(CustomerRestClient customerRestClient) {
        this.customerRestClient = customerRestClient;
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "customerFallback")
    public Customer findCustomerForRead(Long customerId) {
        return callCustomer(customerId);
    }

    /**
     * Fallback for READ enrichment only. Returns null — never fabricates a Customer.
     */
    @SuppressWarnings("unused")
    private Customer customerFallback(Long customerId, Throwable throwable) {
        log.warn("CircuitBreaker customerService READ fallback id={} cause={}",
                customerId, rootMessage(throwable));
        return null;
    }

    /**
     * Strict validation for POST. Fallback never authorizes creation.
     */
    @CircuitBreaker(name = "customerService", fallbackMethod = "requireCustomerFallback")
    public Customer requireCustomerForCreate(Long customerId) {
        return callCustomer(customerId);
    }

    @SuppressWarnings("unused")
    private Customer requireCustomerFallback(Long customerId, Throwable throwable) {
        log.warn("CircuitBreaker customerService CREATE validation refused id={} cause={}",
                customerId, rootMessage(throwable));
        throw new RuntimeException(
                "Customer validation failed — creation refused: " + rootMessage(throwable),
                throwable);
    }

    private Customer callCustomer(Long customerId) {
        try {
            return customerRestClient.findCustomerById(customerId);
        } catch (FeignException exception) {
            if (isMissingCustomer(exception)) {
                throw new CustomerBusinessException(
                        "Customer not found: " + customerId, exception);
            }
            throw exception;
        }
    }

    /**
     * Best-effort distinction without changing Customer-Service.
     * Limitation: Customer-Service returns HTTP 500 for unknown id (same as some real faults).
     */
    private boolean isMissingCustomer(FeignException exception) {
        if (exception.status() == 404) {
            return true;
        }
        String message = exception.getMessage();
        return exception.status() == 500
                && message != null
                && message.toLowerCase().contains("customer not found");
    }

    private static String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
