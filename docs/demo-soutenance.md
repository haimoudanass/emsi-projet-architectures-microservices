# Guide de démonstration — Soutenance (10–15 min)

Prérequis : services démarrés selon [`guide-demarrage-windows.md`](guide-demarrage-windows.md).
Prévoir les délais Eureka (10–60 s) et Circuit Breaker (`waitDuration` 10 s).

---

## Étape A — Architecture (1 min)

| | |
|--|--|
| **Objectif** | Situer Gateway, Eureka, Customer, EBank, 2 H2 |
| **Support** | [`diagrammes/01-architecture-generale.md`](diagrammes/01-architecture-generale.md) |
| **Attendu** | Le jury visualise le flux Client → Gateway → services |
| **Oral** | « Quatre microservices, une entrée unique, découverte Eureka, Feign pour l’enrichissement. » |
| **Risque** | Trop de détail technique trop tôt |

## Étape B — Eureka UP (1 min)

| | |
|--|--|
| **Objectif** | Montrer le registre |
| **URL** | http://localhost:8761/ |
| **Attendu** | CUSTOMER-SERVICE, EBANK-SERVICE, GATEWAY-SERVICE **UP** |
| **Oral** | « Eureka sait où joindre chaque instance. » |
| **Risque** | Instances STARTING : attendre |

## Étape C — Routes Gateway (1 min)

| | |
|--|--|
| **Objectif** | Montrer le routage dynamique |
| **URL** | http://localhost:9999/actuator/gateway/routes |
| **Attendu** | Routes vers CUSTOMER-SERVICE et EBANK-SERVICE |
| **Oral** | « Plus de routes statiques codées en dur : discovery locator. » |
| **Risque** | Endpoint gateway non exposé si mauvaise config (ici OK en local) |

## Étape D — Customers (1 min)

| | |
|--|--|
| **Objectif** | Lister les clients |
| **URL** | http://localhost:9999/CUSTOMER-SERVICE/customers |
| **Attendu** | HTTP 200, ≥ 3 clients |
| **Oral** | « Domaine Customer isolé, base H2 customers-db. » |
| **Risque** | 404 Gateway si Eureka pas encore propagé |

## Étape E — Comptes enrichis (1–2 min)

| | |
|--|--|
| **Objectif** | Montrer OpenFeign |
| **URL** | http://localhost:9999/EBANK-SERVICE/accounts |
| **Attendu** | 15 comptes avec objet `customer` |
| **Oral** | « EBank lit H2 puis appelle Customer via Feign ; `customer` est @Transient. » |
| **Risque** | Enrichissement absent si Customer DOWN |

## Étape F — Création compte valide (1–2 min)

| | |
|--|--|
| **Objectif** | POST réussi |
| **Action** | Créer un customer puis un account avec son `id` réel |
| **Attendu** | HTTP 200, UUID + createdAt + customer |
| **Oral** | « La validation Feign est obligatoire avant l’insert. » |
| **Risque** | Utiliser un customerId inventé → échec |

Exemple body compte :

```json
{"balance":1500,"type":"CURRENT_ACCOUNT","customerId":1}
```

## Étape G — Customer inexistant (1 min)

| | |
|--|--|
| **Objectif** | Refus métier |
| **Action** | `POST .../accounts` avec `"customerId":999999` |
| **Attendu** | Échec HTTP, pas de nouveau compte |
| **Oral** | « Pas de compte orphelin créé. » |
| **Risque** | Confusion si le jury attend un 404 (ici 500 observé) |

## Étape H — Arrêt Customer (30 s)

| | |
|--|--|
| **Objectif** | Simuler la panne |
| **Action** | `Ctrl+C` sur Customer uniquement |
| **Attendu** | Port 8056 libre |
| **Oral** | « On coupe seulement Customer pour montrer la résilience. » |
| **Risque** | Couper Eureka ou EBank par erreur |

## Étape I — GET dégradé (1 min)

| | |
|--|--|
| **Objectif** | Fallback lecture |
| **URL** | http://localhost:9999/EBANK-SERVICE/accounts |
| **Attendu** | HTTP **200**, `customer: null`, soldes présents |
| **Oral** | « Les lectures restent disponibles ; on n’invente pas de client. » |
| **Risque** | Premier appels lents (timeouts Feign) avant OPEN |

## Étape J — POST pendant panne (1 min)

| | |
|--|--|
| **Objectif** | Validation stricte |
| **Action** | POST account `customerId:1` |
| **Attendu** | Échec, compteur de comptes inchangé |
| **Oral** | « Le fallback de création refuse toujours. » |
| **Risque** | Aucun |

## Étape K — Circuit Breaker OPEN (1–2 min)

| | |
|--|--|
| **Objectif** | Preuve d’état |
| **URL** | http://localhost:8057/actuator/circuitbreakers |
| **Attendu** | `"state":"OPEN"`, éventuellement `notPermittedCalls` |
| **Oral** | « Après le seuil, les appels sont court-circuités. » |
| **Risque** | Montrer OPEN trop tôt : faire 3–5 GET détail avant |

## Étape L — Récupération (2–3 min)

| | |
|--|--|
| **Objectif** | HALF_OPEN → CLOSED + enrichissement |
| **Action** | Relancer Customer ; attendre Eureka UP ; attendre ~10 s ; 2 GET succès |
| **Attendu** | Actuator CLOSED ; comptes seed enrichis |
| **Oral** | « Le circuit se referme après des appels de test réussis. » |
| **Risque** | H2 Customer vide → clients créés avant restart disparus (normal) ; se concentrer sur ids 1–3 |

---

## Chronométrage suggéré

| Bloc | Durée |
|------|-------|
| A–C Architecture / Eureka / Gateway | 3 min |
| D–G Fonctionnel | 5 min |
| H–L Résilience | 5–7 min |

## Checklist anti-panne de démo

1. Build OK avant la soutenance.
2. Quatre jars prêts dans `target/`.
3. `init-env.ps1` chargé dans chaque terminal.
4. Navigateur + PowerShell ouverts.
5. Ne pas promettre Kafka, Docker ou Angular (hors périmètre Partie 1).
