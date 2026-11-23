# Rapport final — Architecture Microservices Spring Cloud (EMSI)

## 1. Introduction

Ce projet réalise une architecture microservices locale avec Spring Boot 3.5.10 et Spring Cloud 2025.0.1, dans le cadre pédagogique EMSI (Partie 1). Quatre services collaborent : Discovery (Eureka), Customer, EBank et Gateway.

## 2. Problématique

Comment découper un domaine bancaire simple (clients / comptes) en services autonomes, les faire communiquer de façon découplée, et garantir une dégradation contrôlée lorsqu’un service distant est indisponible — le tout de façon démontrable sur un poste Windows ?

## 3. Objectifs

- Implémenter Customer et EBank avec JPA/H2.
- Introduire Gateway puis Eureka.
- Remplacer le routage statique par un routage dynamique.
- Enrichir les comptes via OpenFeign.
- Protéger les appels avec Resilience4J.
- Valider par build Maven et tests d’intégration réels.
- Documenter pour la soutenance (Phase 8).

## 4. Analyse des besoins

| Besoin | Réponse projet |
|--------|----------------|
| Isolation des données | 2 H2 distinctes |
| Accès unique | Gateway :9999 |
| Découverte | Eureka :8761 |
| Composition lecture | Feign + objet transient |
| Résilience lecture | Fallback `customer=null` |
| Intégrité écriture | POST refusé si validation Customer échoue |

Hors besoin Partie 1 : Kafka, Angular, Docker, Config Server.

## 5. Architecture

Voir [`architecture-technique.md`](architecture-technique.md) et [`diagrammes/01-architecture-generale.md`](diagrammes/01-architecture-generale.md).

Ports : Eureka 8761, Customer 8056, EBank 8057, Gateway 9999.

## 6. Technologies

Java 21, Maven 3.9.6, Spring Boot 3.5.10, Spring Cloud 2025.0.1, Gateway WebFlux, Eureka, OpenFeign, LoadBalancer, Resilience4J, JPA, H2, Springdoc 2.8.13.

## 7. Réalisation des microservices

| Phase | Livrable |
|-------|----------|
| 1 | Customer-Service |
| 2 | EBank-Service |
| 3 | Gateway statique |
| 4 | Eureka + Gateway dynamique |
| 5 | OpenFeign |
| 6 | Resilience4J Circuit Breaker |
| 7 | Tests E2E (30 PASS) |
| 8 | Documentation soutenance |

## 8. Communication interservices

- Client → Gateway → service (`lb://`).
- EBank → Customer via `@FeignClient("CUSTOMER-SERVICE")`.
- Pas d’URL hardcodée Feign : résolution Eureka.

## 9. Résilience

Circuit Breaker `customerService` : CLOSED → OPEN → HALF_OPEN → CLOSED.
Lectures dégradées ; créations strictes. Paramètres démo et preuves Actuator (Phases 6–7).

## 10. Tests et validation

| Type | Résultat |
|------|----------|
| Maven | 25 tests, 0 failure (`4+17+3+1`) |
| Intégration Phase 7 | 30 PASS / 0 FAIL |
| Build | `mvn clean verify` SUCCESS |

Sources de vérité : `logs/phase7/tests-summary.txt`, `logs/phase7/final-test-matrix.md`.

## 11. Difficultés rencontrées

1. PATH Windows avec Java 8 → script `init-env.ps1` session-only.
2. Self-invocation AOP → bean `CustomerLookupService`.
3. Properties Resilience4J non appliquées (window=100) → `CircuitBreakerRegistry` `@Primary`.
4. Délais Eureka / 404 Gateway transitoires.
5. Customer « not found » en HTTP 500 → distinction métier/technique imparfaite.
6. Parsing PowerShell des tableaux JSON lors des tests E2E.

## 12. Solutions adoptées

- Toolchain locale sans modifier les variables Machine/User.
- Seed EBank via repository (indépendant de Feign au boot).
- Fallback lecture null ; fallback création exception.
- Preuves Actuator pour les états CB (pas seulement HTTP 200).
- Documentation des comportements HTTP réels (y compris 500).

## 13. Limites

Détaillées dans [`limitations-et-perspectives.md`](limitations-et-perspectives.md) : H2 mémoire, pas de sécurité prod, pas de Kafka/Docker/Angular dans ce livrable.

## 14. Perspectives

PostgreSQL, Docker Compose, Config Server, Keycloak, monitoring, tracing, Angular, Kafka — **non réalisés** ici.

## 15. Conclusion

Le projet atteint les objectifs pédagogiques de la Partie 1 : microservices Spring Cloud opérationnels en local, communication Feign, résilience Circuit Breaker, validation Maven + E2E, documentation de soutenance. Il constitue une base claire pour une éventuelle Partie 2 (messaging, UI, industrialisation), sans les confondre avec le périmètre actuel.
