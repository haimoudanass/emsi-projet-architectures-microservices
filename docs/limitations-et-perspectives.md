# Limitations et perspectives

## Fonctionnalités réellement implémentées

| Fonctionnalité | Statut |
|----------------|--------|
| Customer-Service REST + H2 | Oui |
| EBank-Service REST + H2 | Oui |
| Eureka Discovery | Oui |
| Gateway WebFlux + discovery locator | Oui |
| OpenFeign EBank → Customer | Oui |
| LoadBalancer | Oui |
| Resilience4J Circuit Breaker | Oui |
| Fallback lecture (`customer=null`) | Oui |
| Validation stricte POST compte | Oui |
| Springdoc Swagger/OpenAPI | Oui |
| Tests Maven (25) | Oui |
| Tests intégration Phase 7 (30) | Oui |

## Limitations actuelles (honnêtes)

1. **H2 en mémoire** : aucune persistance durable ; redémarrage = reset (+ seed).
2. **Pas de SGBD production** (PostgreSQL/MySQL).
3. **Customer inexistant → HTTP 500** : contrat REST perfectible (404 serait plus clair).
4. **Propagation Eureka non instantanée** : délais avant UP / routes Gateway.
5. **Seuils Resilience4J de démonstration** (fenêtre 5, wait 10 s) — adaptés au TP, pas à la prod.
6. **Actuator largement exposé en local** — inacceptable tel quel en production.
7. **Sécurité API absente** (pas d’OAuth2, pas de Keycloak, pas de JWT).
8. **Une instance par service** : pas de haute disponibilité réelle.
9. **Pas de Config Server** : configuration embarquée dans chaque module.
10. **Pas de Docker / CI cloud** dans ce livrable.
11. **Pas de frontend** (Angular hors périmètre).
12. **Pas de Kafka** (TP4 non intégré).
13. **Pas de tracing distribué** (Sleuth/Micrometer Tracing / Zipkin).
14. Après restart Customer, les clients créés dynamiquement disparaissent (H2 mem).

## Perspectives possibles (non réalisées)

Ces éléments sont des **pistes d’évolution**, pas des livrables du projet actuel :

- PostgreSQL ou MySQL par service
- Docker Compose (et éventuellement Kubernetes)
- Spring Cloud Config / Vault
- Sécurité avec Keycloak
- Monitoring Prometheus + Grafana
- Tracing distribué
- Frontend Angular
- Kafka / messaging asynchrone
- Retry / TimeLimiter / Bulkhead Resilience4J
- Pipelines CI/CD

Ne pas les présenter en soutenance comme « déjà faits ».
