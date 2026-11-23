package ma.emsi.microservices.ebankservice.services;

import ma.emsi.microservices.ebankservice.entities.BankAccount;
import ma.emsi.microservices.ebankservice.model.Customer;
import ma.emsi.microservices.ebankservice.repositories.BankAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EBankService {

    private static final Logger log = LoggerFactory.getLogger(EBankService.class);

    private final BankAccountRepository bankAccountRepository;
    private final CustomerLookupService customerLookupService;

    public EBankService(BankAccountRepository bankAccountRepository,
                        CustomerLookupService customerLookupService) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerLookupService = customerLookupService;
    }

    public List<BankAccount> getAllBankAccounts() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        accounts.forEach(this::enrichWithCustomer);
        return accounts;
    }

    public BankAccount getBankAccountById(String id) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        enrichWithCustomer(account);
        return account;
    }

    /**
     * Creation always validates customer remotely.
     * No success fallback: unknown customer or Customer DOWN → refuse, no H2 insert.
     */
    public BankAccount saveBankAccount(BankAccount account) {
        try {
            Customer customer = customerLookupService.requireCustomerForCreate(account.getCustomerId());
            account.setId(UUID.randomUUID().toString());
            account.setCreatedAt(new Date());
            BankAccount saved = bankAccountRepository.save(account);
            saved.setCustomer(customer);
            return saved;
        } catch (CustomerBusinessException businessException) {
            throw new RuntimeException(businessException.getMessage(), businessException);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    private void enrichWithCustomer(BankAccount account) {
        try {
            Customer customer = customerLookupService.findCustomerForRead(account.getCustomerId());
            account.setCustomer(customer);
        } catch (CustomerBusinessException businessException) {
            log.warn("Read enrichment: business miss for customerId={} — customer left null",
                    account.getCustomerId());
            account.setCustomer(null);
        }
    }
}
