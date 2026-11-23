# Diagramme — Architecture générale

```mermaid
flowchart LR
  Client[Client HTTP]

  subgraph edge [Point d'entree]
    GW[Gateway-Service :9999]
  end

  subgraph discovery [Discovery]
    EU[Eureka Server :8761]
  end

  subgraph domain [Services metier]
    CS[Customer-Service :8056]
    EB[EBank-Service :8057]
  end

  subgraph data [Bases H2 independantes]
    H2C[(customers-db)]
    H2E[(accounts-db)]
  end

  Client -->|HTTP| GW
  GW -->|lb CUSTOMER-SERVICE| CS
  GW -->|lb EBANK-SERVICE| EB
  CS --- EU
  EB --- EU
  GW --- EU
  CS --> H2C
  EB --> H2E
  EB -->|OpenFeign + Resilience4J| CS
```

## Lecture

- Le client passe en principe par la Gateway.
- Eureka connaît les instances UP.
- EBank appelle Customer via Feign protégé par le Circuit Breaker `customerService`.
- Deux H2 mémoire distinctes : pas de jointure SQL interservices.
