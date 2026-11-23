# Référence API

Endpoints réellement implémentés (pas de PUT/DELETE métier).
Exemples issus des preuves Phase 7 (`logs/phase7/`).

---

## Customer-Service

### GET /customers

| | |
|--|--|
| Direct | `http://localhost:8056/customers` |
| Gateway | `http://localhost:9999/CUSTOMER-SERVICE/customers` |
| Réponse observée | HTTP **200** |

```json
[
  {"id":1,"name":"Mohammed","email":"mohammed@example.com"},
  {"id":2,"name":"Imane","email":"imane@example.com"},
  {"id":3,"name":"Yassine","email":"yassine@example.com"}
]
```

### GET /customers/{id}

| | |
|--|--|
| Direct | `http://localhost:8056/customers/1` |
| Gateway | `http://localhost:9999/CUSTOMER-SERVICE/customers/1` |
| Succès | HTTP **200** |
| ID inconnu (ex. 999999) | HTTP **500** observé (pas 404) |

```json
{"id":1,"name":"Mohammed","email":"mohammed@example.com"}
```

### POST /customers

| | |
|--|--|
| Direct | `http://localhost:8056/customers` |
| Gateway | `http://localhost:9999/CUSTOMER-SERVICE/customers` |
| Body | `{"name":"Phase7Demo","email":"phase7demo@example.com"}` |
| Succès | HTTP **200** |

```json
{"id":4,"name":"Phase7Demo","email":"phase7demo@example.com"}
```

---

## EBank-Service

### GET /accounts

| | |
|--|--|
| Direct | `http://localhost:8057/accounts` |
| Gateway | `http://localhost:9999/EBANK-SERVICE/accounts` |
| Customer UP | HTTP **200**, comptes enrichis (`customer` non null) |
| Customer DOWN | HTTP **200**, `customer: null`, données H2 conservées |

Exemple détail enrichi :

```json
{
  "id": "6ffcc53b-c6b4-49af-a6e9-fc6aa201efd9",
  "createdAt": "2026-11-23T00:21:37.006+00:00",
  "balance": 68248.52592371599,
  "type": "CURRENT_ACCOUNT",
  "customerId": 1,
  "customer": {
    "id": 1,
    "name": "Mohammed",
    "email": "mohammed@example.com"
  }
}
```

Exemple panne (`customer` null) :

```json
{
  "id": "6ffcc53b-c6b4-49af-a6e9-fc6aa201efd9",
  "createdAt": "2026-11-23T00:21:37.006+00:00",
  "balance": 68248.52592371599,
  "type": "CURRENT_ACCOUNT",
  "customerId": 1,
  "customer": null
}
```

### GET /accounts/{id}

| | |
|--|--|
| Direct | `http://localhost:8057/accounts/{uuid}` |
| Gateway | `http://localhost:9999/EBANK-SERVICE/accounts/{uuid}` |
| Compte existant | HTTP **200** (même en panne Customer) |
| UUID inconnu | HTTP **500** observé |

### POST /accounts

| | |
|--|--|
| Direct | `http://localhost:8057/accounts` |
| Gateway | `http://localhost:9999/EBANK-SERVICE/accounts` |
| Body | `{"balance":7777.7,"type":"SAVING_ACCOUNT","customerId":4}` |

Succès (client valide + Customer UP) — HTTP **200** :

```json
{
  "id": "00c86f6c-5c89-45cc-b5b4-175fe65f477d",
  "createdAt": "2026-11-23T00:23:04.751+00:00",
  "balance": 7777.7,
  "type": "SAVING_ACCOUNT",
  "customerId": 4,
  "customer": {
    "id": 4,
    "name": "Phase7Demo",
    "email": "phase7demo@example.com"
  }
}
```

Échecs observés (aucune insertion H2) :

| Cas | HTTP observé |
|-----|--------------|
| `customerId` inexistant (999999) | 500 |
| `customerId` null | 500 |
| Customer-Service DOWN / CB OPEN | 500 |

---

## Gateway — routes dynamiques

| Motif | Cible |
|-------|-------|
| `/CUSTOMER-SERVICE/**` | `lb://CUSTOMER-SERVICE` |
| `/EBANK-SERVICE/**` | `lb://EBANK-SERVICE` |

| URL | Résultat Phase 7 |
|-----|------------------|
| `/UNKNOWN-SERVICE/test` | HTTP **404** |
| `/accounts` | HTTP **404** |
| `/customers` | HTTP **404** |

Lister les routes (local) : `GET http://localhost:9999/actuator/gateway/routes`

---

## Actuator utile (EBank, local)

| Endpoint | Usage |
|----------|--------|
| `/actuator/health` | Santé |
| `/actuator/circuitbreakers` | État `customerService` |
| `/actuator/circuitbreakerevents` | Événements CB |

---

## Swagger

- Customer : http://localhost:8056/swagger-ui/index.html
- EBank : http://localhost:8057/swagger-ui/index.html
