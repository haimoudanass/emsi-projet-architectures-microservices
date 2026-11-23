# Architecture technique

## Vue d’ensemble

Quatre modules Maven sous le parent `ma.emsi.microservices:microservices-project:1.0.0-SNAPSHOT` :

| Module | Artifact | Port | Stack principale |
|--------|----------|------|------------------|
| Discovery | `discovery-service` | 8761 | Eureka Server |
| Customer | `customer-service` | 8056 | Web, JPA, H2, Eureka Client, Springdoc |
| EBank | `ebank-service` | 8057 | Web, JPA, H2, Eureka, OpenFeign, LoadBalancer, Resilience4J, Springdoc |
| Gateway | `gateway-service` | 9999 | Gateway WebFlux, Eureka Client |

## Rôles et responsabilités

### Discovery-Service

- Héberge le registre Eureka.
- Ne s’enregistre pas (`register-with-eureka=false`, `fetch-registry=false`).
- Dashboard : `http://localhost:8761/`.

### Customer-Service

- Entité `Customer` (id, name, email).
- Endpoints : `GET /customers`, `GET /customers/{id}`, `POST /customers`.
- Base H2 `customers-db`.
- Seed initial : 3 clients (Mohammed, Imane, Yassine).
- Comportement observé : ID inconnu → HTTP **500** (pas 404).

### EBank-Service

- Entité `BankAccount` : id (UUID), createdAt, balance, type, customerId.
- Champ `customer` annoté `@Transient` : jamais persisté en H2.
- Seed : 15 comptes (customerId 1–3).
- `CustomerRestClient` (Feign) + `CustomerLookupService` (`@CircuitBreaker`).
- Lectures enrichies ; POST valide le client distant **avant** `save`.

### Gateway-Service

- Point d’entrée unique.
- Discovery locator activé → routes `/{spring.application.name en majuscules}/**`.
- Exemples : `/CUSTOMER-SERVICE/customers`, `/EBANK-SERVICE/accounts`.

## Dépendances clés (réelles)

- **Customer** : `spring-boot-starter-web`, `data-jpa`, `actuator`, `h2`, Eureka Client, springdoc, lombok.
- **EBank** : idem + OpenFeign, LoadBalancer, `spring-cloud-starter-circuitbreaker-resilience4j`, `resilience4j-spring-boot3`, `spring-boot-starter-aop`.
- **Gateway** : `spring-cloud-starter-gateway-server-webflux`, Eureka Client.
- **Discovery** : `spring-cloud-starter-netflix-eureka-server`.

## Quatre modes d’appel (à distinguer)

### 1. Appel direct

Le client HTTP parle au microservice sans passer par la Gateway :

`GET http://localhost:8057/accounts`

Utile en debug local. Ne passe pas par Eureka côté client externe.

### 2. Appel via Gateway

`GET http://localhost:9999/EBANK-SERVICE/accounts`

La Gateway résout le nom de service via Eureka, puis balance la requête (`lb://EBANK-SERVICE`).

### 3. Appel OpenFeign (interservices)

EBank appelle Customer **sans URL fixe** :

```java
@FeignClient(name = "CUSTOMER-SERVICE")
Customer findCustomerById(Long id);
```

Feign + LoadBalancer interrogent Eureka pour obtenir l’instance.

### 4. Découverte Eureka

Mécanisme transverse : enregistrement périodique des instances, heartbeats, cache côté clients.
La propagation n’est **pas** instantanée (quelques secondes après démarrage ou redémarrage).

## Séparation des données

- Customer et EBank ont des H2 **distinctes**.
- EBank ne stocke que `customerId` (clé logique).
- Le JSON de réponse peut contenir l’objet `customer` hydraté à la volée.

## Circuit Breaker (instance `customerService`)

| Paramètre | Valeur TP |
|-----------|-----------|
| slidingWindowType | COUNT_BASED |
| slidingWindowSize | 5 |
| minimumNumberOfCalls | 3 |
| failureRateThreshold | 50 % |
| waitDurationInOpenState | 10 s |
| permittedNumberOfCallsInHalfOpenState | 2 |

Implémentation :

- Annotation `@CircuitBreaker` sur `CustomerLookupService` (bean séparé → proxy AOP).
- Lecture : fallback `null`.
- Création : fallback qui **lève** une exception.
- `CircuitBreakerRegistry` `@Primary` force les seuils démo (les properties seules pouvaient rester aux défauts window=100).
- `spring.cloud.openfeign.circuitbreaker.enabled=false` (pas de CB global non contrôlé).

## Observabilité locale

EBank expose (profil local, non production) :

- `/actuator/health`
- `/actuator/circuitbreakers`
- `/actuator/circuitbreakerevents`

Gateway expose `/actuator/gateway/routes` (accès local).

## Ce qui n’existe pas dans ce projet

- Config Server, Kafka, Docker Compose, frontend Angular, sécurité OAuth2/Keycloak, DELETE/PUT sur les ressources métier.
