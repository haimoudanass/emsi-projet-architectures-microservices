package ma.emsi.microservices.ebankservice.services;

/**
 * Business miss (unknown customer). Not a transport/outage failure.
 * Configured as Resilience4J ignoreExceptions so it does not open the circuit
 * and is not silently converted into a "network down" success path.
 */
public class CustomerBusinessException extends RuntimeException {

    public CustomerBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
