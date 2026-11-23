package ma.emsi.microservices.ebankservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Non-JPA projection of a remote customer.
 * Will be populated via OpenFeign in a later phase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    private Long id;
    private String name;
    private String email;
}
