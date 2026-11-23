# Diagramme — Déploiement local (ordre de démarrage)

```mermaid
flowchart TD
  A[1. Charger init-env.ps1] --> B[2. mvn clean verify]
  B --> C[3. Demarrer discovery-service :8761]
  C --> D{Eureka dashboard HTTP 200?}
  D -->|Non| C
  D -->|Oui| E[4. Demarrer customer-service :8056]
  E --> F[5. Demarrer ebank-service :8057]
  F --> G[6. Demarrer gateway-service :9999]
  G --> H{Eureka: CUSTOMER EBANK GATEWAY UP?}
  H -->|Non| I[Attendre heartbeats 10-60 s]
  I --> H
  H -->|Oui| J{Gateway /CUSTOMER-SERVICE/customers = 200?}
  J -->|404 transitoire| K[Attendre cache discovery locator]
  K --> J
  J -->|Oui| L[Pret pour demos et tests HTTP]
```

## Remarques

- Ne pas démarrer EBank avant Eureka si l’on veut une découverte immédiate (EBank tolère un démarrage parallèle mais Feign nécessite Customer UP pour l’enrichissement).
- Un premier `404` Gateway juste après l’enregistrement Eureka a été observé en Phase 7 : patienter puis retester.
- Arrêt : stopper uniquement les processus Java démarrés pour la démo ; vérifier ports 8056, 8057, 8761, 9999 libres.
