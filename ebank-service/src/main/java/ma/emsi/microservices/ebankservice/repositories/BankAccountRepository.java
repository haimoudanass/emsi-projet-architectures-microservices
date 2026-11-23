package ma.emsi.microservices.ebankservice.repositories;

import ma.emsi.microservices.ebankservice.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
}
