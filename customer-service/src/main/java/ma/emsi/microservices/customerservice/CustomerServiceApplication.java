package ma.emsi.microservices.customerservice;

import ma.emsi.microservices.customerservice.entities.Customer;
import ma.emsi.microservices.customerservice.services.CustomerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(CustomerService customerService) {
        return args -> {
            if (customerService.getAllCustomers().isEmpty()) {
                List.of("Mohammed", "Imane", "Yassine").forEach(name ->
                        customerService.saveCustomer(
                                Customer.builder()
                                        .name(name)
                                        .email(name.toLowerCase() + "@example.com")
                                        .build()
                        )
                );
            }
        };
    }
}
