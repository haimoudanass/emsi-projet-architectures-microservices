# EMSI — Projet – Architectures Microservices

Architecture microservices locale basée sur **Spring Boot 3.5.10** et **Spring Cloud 2025.0.1**, réalisée en 7 phases techniques + documentation (Phase 8).

**Dépôt GitHub cible :** `emsi-projet-architectures-microservices`

---

## Description technique

Le projet simule un domaine bancaire simple :

- **Customer-Service** gère les clients
- **EBank-Service** gère les comptes et enrichit chaque compte avec le client distant
- **Gateway-Service** expose un point d’entrée unique (routage dynamique via Eureka)
- **Discovery-Service** (Netflix Eureka) enregistre et découvre les instances

Objectif : construire et démontrer les briques Spring Cloud essentielles, sans Kafka (périmètre TP4 séparé).

## Objectif pédagogique

- Découper un monolithe conceptuel en microservices
- Exposer des API REST avec Spring Web + JPA + H2
- Centraliser l’accès via une API Gateway
- Utiliser Eureka pour la découverte de services
- Passer du routage statique au routage dynamique (`lb://`)
- Consommer un service distant avec OpenFeign
- Protéger les appels avec Resilience4J Circuit Breaker
- Valider par tests Maven et tests d’intégration HTTP

## Technologies

| Composant | Version / choix |
|-----------|-----------------|
| Java | 21 (Temurin recommandé) |
| Maven | 3.9.6 (ou compatible 3.9.x) |
| Spring Boot | 3.5.10 |
| Spring Cloud | 2025.0.1 |
| Gateway | Spring Cloud Gateway WebFlux |
| Discovery | Netflix Eureka Server/Client |
| Client HTTP | OpenFeign + LoadBalancer |
| Résilience | Resilience4J Circuit Breaker |
| Persistance | Spring Data JPA + H2 (mémoire) |
| Documentation API | Springdoc OpenAPI 2.8.13 |

Vérifié dans le `pom.xml` parent et les modules.

## Fonctionnalités

- CRUD clients (Customer)
- Comptes bancaires + enrichissement Feign (EBank)
- Validation client avant création de compte
- Circuit Breaker avec fallback contrôlé (lecture `customer=null`, écriture refusée)
- Gateway dynamique : `/{SERVICE-ID}/**`
- Swagger UI sur Customer et EBank
- 25 tests Maven (`@Test`) dans les modules
- Matrice d’intégration Phase 7 documentée localement (30 scénarios) — journaux sous `logs/` non publiés

## Architecture

```text
Client HTTP
    │
    ▼
Gateway-Service :9999  ──►  Eureka :8761
    │                            ▲
    ├── /CUSTOMER-SERVICE/** ────┤
    └── /EBANK-SERVICE/** ───────┤
              │                  │
              ▼                  │
        EBank :8057 ──Feign+CB──► Customer :8056
              │                        │
         H2 accounts-db           H2 customers-db
```

Diagrammes : [`docs/diagrammes/`](docs/diagrammes/).

## Structure des dossiers

```text
microservices-project/
├── pom.xml
├── README.md
├── .gitignore
├── customer-service/
├── ebank-service/
├── gateway-service/
├── discovery-service/
├── scripts/
│   ├── init-env.ps1          # helper local Windows (optionnel)
│   └── phase7/
└── docs/
    ├── architecture-technique.md
    ├── api-reference.md
    ├── guide-demarrage-windows.md
    ├── demo-soutenance.md
    ├── questions-soutenance.md
    ├── limitations-et-perspectives.md
    ├── rapport-final.md
    └── diagrammes/
```

Le dossier `logs/` peut exister en local (preuves de phases) ; il est **exclu** du dépôt public.

## Prérequis

- **JDK 21** installé (`java -version` → 21)
- **Maven 3.9.6** (ou 3.9.x) disponible dans le `PATH` de la session
- PowerShell (Windows) ou shell équivalent

### Option A — Portable (recommandée pour clone GitHub)

Aucun dossier Desktop externe n’est requis :

```powershell
java -version    # 21
mvn -version     # 3.9.x
```

Si besoin, définir uniquement pour la session :

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;C:\Path\To\apache-maven-3.9.6\bin;" + $env:Path
```

### Option B — Helper local de ce poste (`scripts/init-env.ps1`)

Sur la machine de développement actuelle, un script charge un JDK 21 et Maven 3.9.6 déjà installés localement.
**Ce script n’est pas modifié** (chemins machine préservés pour ne pas casser l’environnement actif).

```powershell
cd microservices-project
. .\scripts\init-env.ps1
```

Les clones sur d’autres machines doivent privilégier l’**Option A**.

## Installation

```powershell
cd microservices-project
mvn clean verify
```

Résultat attendu (Phases 7/8) : **BUILD SUCCESS**, **25** tests Maven (4 + 17 + 3 + 1).

## Démarrage

Ordre obligatoire :

1. Discovery (`:8761`)
2. Customer (`:8056`)
3. EBank (`:8057`)
4. Gateway (`:9999`)

Attendre Eureka **UP** pour `CUSTOMER-SERVICE`, `EBANK-SERVICE`, `GATEWAY-SERVICE`.
Guide : [`docs/guide-demarrage-windows.md`](docs/guide-demarrage-windows.md) (certains chemins absolus locaux peuvent encore y figurer ; adapter au clone).

Exemple :

```powershell
cd discovery-service
mvn spring-boot:run
```

## Exemples d’utilisation

```text
GET  http://localhost:8761/
GET  http://localhost:8056/customers
GET  http://localhost:9999/CUSTOMER-SERVICE/customers
GET  http://localhost:8057/accounts
GET  http://localhost:9999/EBANK-SERVICE/accounts
POST http://localhost:9999/CUSTOMER-SERVICE/customers
POST http://localhost:9999/EBANK-SERVICE/accounts
```

Référence : [`docs/api-reference.md`](docs/api-reference.md).

## Swagger / OpenAPI

| Service | Swagger UI |
|---------|------------|
| Customer | http://localhost:8056/swagger-ui/index.html |
| EBank | http://localhost:8057/swagger-ui/index.html |

## Tests

```powershell
mvn test
# ou
mvn clean verify
```

- **Maven** : 25 tests unitaires/intégration Spring (modules) — recensés dans `src/test`
- **Phase 7** : 30 scénarios HTTP / résilience — résultats historiques locaux sous `logs/` (non publiés)
- Script local : `scripts/phase7/run-integration-tests.ps1`

## Gestion des pannes

| Situation | Lecture comptes | Création compte |
|-----------|-----------------|-----------------|
| Customer UP | Comptes enrichis | Autorisée si client existe |
| Customer DOWN / CB OPEN | HTTP 200, `customer=null` | Refusée (pas d’insert) |
| Client inexistant | Enrichissement null / erreur métier | Refusée |

Actuator EBank (local) : `/actuator/circuitbreakers`.

## Limites connues

Voir [`docs/limitations-et-perspectives.md`](docs/limitations-et-perspectives.md) :

- H2 mémoire
- Pas de sécurité industrialisée
- Pas de Kafka / Docker / Config Server dans ce projet
- `scripts/init-env.ps1` reste spécifique au poste local

## Références pédagogiques

- Cours / atelier EMSI — Architecture Microservices Spring Cloud (Partie 1)
- Support associé OpenFeign / Resilience4J
- Documentation Spring Cloud Gateway, Eureka, OpenFeign, Resilience4J

### Documentation soutenance

| Document | Chemin |
|----------|--------|
| Architecture | [`docs/architecture-technique.md`](docs/architecture-technique.md) |
| Démo 10–15 min | [`docs/demo-soutenance.md`](docs/demo-soutenance.md) |
| Questions / réponses | [`docs/questions-soutenance.md`](docs/questions-soutenance.md) |
| Rapport final | [`docs/rapport-final.md`](docs/rapport-final.md) |
