# Phase 7 — Tests d'intégration

## Exécution

```powershell
. .\scripts\init-env.ps1
# Après mvn clean verify :
powershell -ExecutionPolicy Bypass -File .\scripts\phase7\run-integration-tests.ps1
```

Preuves : `logs/phase7/`
Collection Postman (optionnelle) : `docs/phase7/EMSI-Phase7.postman_collection.json`

## Ordre de démarrage

1. Discovery :8761
2. Customer :8056
3. EBank :8057
4. Gateway :9999

Attendre Eureka UP puis routes Gateway (un premier 404 dynamique peut précéder le 200).
