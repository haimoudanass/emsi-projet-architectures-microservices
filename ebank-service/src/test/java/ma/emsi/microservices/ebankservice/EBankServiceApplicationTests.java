package ma.emsi.microservices.ebankservice;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import ma.emsi.microservices.ebankservice.clients.CustomerRestClient;
import ma.emsi.microservices.ebankservice.entities.BankAccount;
import ma.emsi.microservices.ebankservice.model.Customer;
import ma.emsi.microservices.ebankservice.repositories.BankAccountRepository;
import ma.emsi.microservices.ebankservice.services.EBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(EBankServiceApplicationTests.EarlyFeignStubs.class)
class EBankServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EBankService eBankService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private CustomerRestClient customerRestClient;

    @TestConfiguration
    static class EarlyFeignStubs {
        @Autowired
        private CustomerRestClient customerRestClient;

        @PostConstruct
        void stubDefaultCustomers() {
            stubValidCustomers(customerRestClient);
            when(customerRestClient.findCustomerById(999999L)).thenThrow(notFound(999999L));
        }
    }

    @BeforeEach
    void stubCustomers() {
        reset(customerRestClient);
        stubValidCustomers(customerRestClient);
        when(customerRestClient.findCustomerById(999999L)).thenThrow(notFound(999999L));
        circuitBreakerRegistry.circuitBreaker("customerService").reset();
    }

    @Test
    void contextLoadsAndSeedsFifteenAccounts() {
        assertThat(eBankService.getAllBankAccounts()).hasSizeGreaterThanOrEqualTo(15);
    }

    @Test
    void getAllBankAccountsReturnsSeededDataEnriched() {
        List<BankAccount> accounts = eBankService.getAllBankAccounts().stream()
                .filter(account -> account.getCustomerId() != null && account.getCustomerId() <= 3L)
                .toList();
        assertThat(accounts).hasSizeGreaterThanOrEqualTo(15);
        assertThat(accounts).allSatisfy(account -> {
            assertThat(account.getId()).isNotBlank();
            assertThat(account.getCreatedAt()).isNotNull();
            assertThat(account.getCustomerId()).isBetween(1L, 3L);
            assertThat(account.getType()).isIn("CURRENT_ACCOUNT", "SAVING_ACCOUNT");
            assertThat(account.getBalance()).isPositive();
            assertThat(account.getCustomer()).isNotNull();
            assertThat(account.getCustomer().getId()).isEqualTo(account.getCustomerId());
        });
    }

    @Test
    void saveBankAccountGeneratesUuidAndCreatedAtForValidCustomer() {
        BankAccount saved = eBankService.saveBankAccount(
                BankAccount.builder()
                        .balance(15000.0)
                        .type("CURRENT_ACCOUNT")
                        .customerId(1L)
                        .build()
        );
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCustomerId()).isEqualTo(1L);
        assertThat(saved.getCustomer()).isNotNull();
        assertThat(saved.getCustomer().getName()).isEqualTo("Mohammed");
    }

    @Test
    void getBankAccountByIdReturnsExistingAccountEnriched() {
        BankAccount existing = bankAccountRepository.findAll().getFirst();
        BankAccount found = eBankService.getBankAccountById(existing.getId());
        assertThat(found.getId()).isEqualTo(existing.getId());
        assertThat(found.getCustomer()).isNotNull();
        assertThat(found.getCustomer().getId()).isEqualTo(found.getCustomerId());
    }

    @Test
    void getBankAccountByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> eBankService.getBankAccountById("unknown-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void getAccountsEndpointReturnsOkWithCustomer() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].customer.id").exists())
                .andExpect(jsonPath("$[0].customer.name").exists());
    }

    @Test
    void postAccountEndpointCreatesAccountForValidCustomer() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"balance":15000,"type":"CURRENT_ACCOUNT","customerId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customer.name").value("Mohammed"));
    }

    @Test
    void saveBankAccountRejectsUnknownCustomerWithoutPersisting() {
        long before = bankAccountRepository.count();
        assertThatThrownBy(() -> eBankService.saveBankAccount(
                BankAccount.builder()
                        .balance(1000.0)
                        .type("SAVING_ACCOUNT")
                        .customerId(999999L)
                        .build()
        )).isInstanceOf(RuntimeException.class);
        assertThat(bankAccountRepository.count()).isEqualTo(before);
    }

    @Test
    void saveBankAccountFailsWhenCustomerServiceUnavailableWithoutFallback() {
        when(customerRestClient.findCustomerById(1L)).thenThrow(unavailable());
        long before = bankAccountRepository.count();
        assertThatThrownBy(() -> eBankService.saveBankAccount(
                BankAccount.builder()
                        .balance(1000.0)
                        .type("CURRENT_ACCOUNT")
                        .customerId(1L)
                        .build()
        )).isInstanceOf(RuntimeException.class);
        assertThat(bankAccountRepository.count()).isEqualTo(before);
        verify(customerRestClient).findCustomerById(1L);
    }

    @Test
    void enrichmentUsesFallbackNullWhenCustomerServiceUnavailable() {
        BankAccount existing = bankAccountRepository.findAll().getFirst();
        double balance = existing.getBalance();
        String type = existing.getType();
        Long customerId = existing.getCustomerId();
        when(customerRestClient.findCustomerById(anyLong())).thenThrow(unavailable());

        BankAccount found = eBankService.getBankAccountById(existing.getId());

        assertThat(found.getId()).isEqualTo(existing.getId());
        assertThat(found.getBalance()).isEqualTo(balance);
        assertThat(found.getType()).isEqualTo(type);
        assertThat(found.getCustomerId()).isEqualTo(customerId);
        assertThat(found.getCustomer()).isNull();
        verify(customerRestClient).findCustomerById(customerId);
    }

    @Test
    void getAllAccountsKeepsH2DataWhenCustomerUnavailable() {
        when(customerRestClient.findCustomerById(anyLong())).thenThrow(unavailable());
        List<BankAccount> accounts = eBankService.getAllBankAccounts();
        assertThat(accounts).hasSizeGreaterThanOrEqualTo(15);
        assertThat(accounts).allSatisfy(account -> {
            assertThat(account.getId()).isNotBlank();
            assertThat(account.getBalance()).isPositive();
            assertThat(account.getCustomerId()).isBetween(1L, 3L);
            assertThat(account.getCustomer()).isNull();
        });
        verify(customerRestClient, atLeastOnce()).findCustomerById(anyLong());
    }

    @Test
    void unknownCustomerDoesNotInventFakeCustomerOnRead() {
        BankAccount orphan = bankAccountRepository.save(BankAccount.builder()
                .id("orphan-read-id")
                .balance(99.0)
                .type("CURRENT_ACCOUNT")
                .customerId(999999L)
                .createdAt(new java.util.Date())
                .build());
        try {
            BankAccount found = eBankService.getBankAccountById(orphan.getId());
            assertThat(found.getCustomer()).isNull();
            assertThat(found.getCustomerId()).isEqualTo(999999L);
        } finally {
            bankAccountRepository.deleteById(orphan.getId());
        }
    }

    @Test
    void transientCustomerIsNotPersistedInH2() {
        BankAccount saved = eBankService.saveBankAccount(
                BankAccount.builder()
                        .balance(2500.0)
                        .type("SAVING_ACCOUNT")
                        .customerId(2L)
                        .build()
        );
        assertThat(saved.getCustomer()).isNotNull();
        BankAccount reloaded = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCustomer()).isNull();
        assertThat(reloaded.getCustomerId()).isEqualTo(2L);
        verify(customerRestClient).findCustomerById(2L);
        verify(customerRestClient, never()).findCustomerById(999999L);
    }

    @Test
    void h2SchemaUnchanged_columnsOnlyBankAccount() {
        BankAccount row = bankAccountRepository.findAll().getFirst();
        assertThat(row.getId()).isNotBlank();
        assertThat(row.getBalance()).isNotNull();
        assertThat(row.getType()).isNotBlank();
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getCustomerId()).isNotNull();
        assertThat(row.getCustomer()).isNull();
    }

    @Test
    void circuitBreakerClosedUnderNormalOperation() {
        eBankService.getAllBankAccounts();
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("customerService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreakerOpensAfterConfiguredFailureThreshold() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("customerService");
        assertThat(cb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(5);
        assertThat(cb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(3);

        when(customerRestClient.findCustomerById(anyLong())).thenThrow(unavailable());
        BankAccount existing = bankAccountRepository.findAll().getFirst();

        for (int i = 0; i < 5; i++) {
            eBankService.getBankAccountById(existing.getId());
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isGreaterThanOrEqualTo(3);

        reset(customerRestClient);
        when(customerRestClient.findCustomerById(anyLong())).thenReturn(customer(1L, "X", "x@x.com"));
        BankAccount shortCircuited = eBankService.getBankAccountById(existing.getId());
        assertThat(shortCircuited.getCustomer()).isNull();
        verify(customerRestClient, never()).findCustomerById(anyLong());
    }

    @Test
    void circuitBreakerHalfOpenThenClosedAfterRecovery() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("customerService");
        when(customerRestClient.findCustomerById(anyLong())).thenThrow(unavailable());
        BankAccount existing = bankAccountRepository.findAll().getFirst();
        for (int i = 0; i < 5; i++) {
            eBankService.getBankAccountById(existing.getId());
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Deterministic transition (avoid fragile waitDuration sleep)
        cb.transitionToHalfOpenState();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        reset(customerRestClient);
        stubValidCustomers(customerRestClient);
        eBankService.getBankAccountById(existing.getId());
        eBankService.getBankAccountById(existing.getId());

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verify(customerRestClient, times(2)).findCustomerById(existing.getCustomerId());
    }

    private static void stubValidCustomers(CustomerRestClient client) {
        when(client.findCustomerById(1L)).thenReturn(customer(1L, "Mohammed", "mohammed@example.com"));
        when(client.findCustomerById(2L)).thenReturn(customer(2L, "Imane", "imane@example.com"));
        when(client.findCustomerById(3L)).thenReturn(customer(3L, "Yassine", "yassine@example.com"));
    }

    private static Customer customer(Long id, String name, String email) {
        return Customer.builder().id(id).name(name).email(email).build();
    }

    private static FeignException notFound(Long id) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/customers/" + id,
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return new FeignException.InternalServerError("Customer not found", request, null, Collections.emptyMap());
    }

    private static FeignException unavailable() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/customers/1",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return new FeignException.ServiceUnavailable(
                "Customer service unavailable",
                request,
                "down".getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );
    }
}
