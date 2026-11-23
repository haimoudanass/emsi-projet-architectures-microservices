package ma.emsi.microservices.ebankservice;

import ma.emsi.microservices.ebankservice.entities.BankAccount;
import ma.emsi.microservices.ebankservice.repositories.BankAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
public class EBankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EBankServiceApplication.class, args);
    }

    /**
     * Demo seed persists directly to H2 (no Feign).
     * Public API saveBankAccount() still validates customerId via OpenFeign.
     * This avoids a startup race where Eureka cache is still empty.
     */
    @Bean
    CommandLineRunner start(BankAccountRepository bankAccountRepository) {
        return args -> {
            if (!bankAccountRepository.findAll().isEmpty()) {
                return;
            }
            Random random = new Random();
            for (long customerId = 1; customerId <= 3; customerId++) {
                for (int j = 1; j <= 5; j++) {
                    String type = random.nextBoolean() ? "CURRENT_ACCOUNT" : "SAVING_ACCOUNT";
                    double balance = 1000 + random.nextDouble() * 90000;
                    bankAccountRepository.save(
                            BankAccount.builder()
                                    .id(UUID.randomUUID().toString())
                                    .createdAt(new Date())
                                    .balance(balance)
                                    .type(type)
                                    .customerId(customerId)
                                    .build()
                    );
                }
            }
        };
    }
}
