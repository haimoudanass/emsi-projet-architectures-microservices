# Diagramme — Consultation des comptes enrichis

```mermaid
sequenceDiagram
  actor Client
  participant GW as Gateway :9999
  participant EU as Eureka :8761
  participant EB as EBank :8057
  participant CB as CircuitBreaker customerService
  participant CS as Customer :8056

  Client->>GW: GET /EBANK-SERVICE/accounts
  GW->>EU: Resoudre EBANK-SERVICE
  EU-->>GW: Instance :8057
  GW->>EB: GET /accounts
  EB->>EB: Lecture H2 accounts-db
  loop Pour chaque compte
    EB->>CB: findCustomerForRead(customerId)
    CB->>CS: Feign GET /customers/{id}
    CS-->>CB: Customer JSON
    CB-->>EB: Customer
    EB->>EB: account.customer = Customer
  end
  EB-->>GW: Liste comptes enrichis
  GW-->>Client: HTTP 200 JSON
```

## Points clés

- Enrichissement **à la lecture**, pas en base.
- Le champ `customer` est `@Transient`.
- Même flux en appel direct `GET http://localhost:8057/accounts` (sans Gateway).
