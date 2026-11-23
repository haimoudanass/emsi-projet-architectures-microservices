package ma.emsi.microservices.customerservice.repositories;

import ma.emsi.microservices.customerservice.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
