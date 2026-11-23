# Questions de soutenance (avec réponses)

## 1. Qu’est-ce qu’un microservice ?

Un service autonome, déployable séparément, centré sur une responsabilité métier (ici Customer ou Comptes), communiquant par HTTP/API plutôt que par une seule base partagée.

## 2. Pourquoi Spring Boot ?

Il accélère la création d’applications Java : serveur embarqué, configuration par conventions, starters Maven, idéal pour des microservices pédagogiques.

## 3. Quel est le rôle d’Eureka ?

Eureka est un **registre de services**. Les instances s’y enregistrent ; les clients (Gateway, Feign) y découvrent les adresses à jour.

## 4. Pourquoi une Gateway ?

Point d’entrée unique pour le client : routage, découplage des ports internes, évolution possible (filtres, sécurité) sans exposer chaque service.

## 5. Routage statique vs dynamique ?

- **Statique** : URLs/host:port codés dans la Gateway.
- **Dynamique** : la Gateway interroge Eureka et route via `lb://NOM-SERVICE` (discovery locator).

## 6. Que signifie `lb://` ?

**Load Balancer** URI : Spring Cloud LoadBalancer choisit une instance saine du service nommé dans Eureka.

## 7. À quoi sert LoadBalancer ?

Répartir les appels entre instances d’un même service (ici une instance par service, mais le mécanisme est en place).

## 8. Qu’est-ce qu’OpenFeign ?

Un client HTTP déclaratif : une interface Java annotée génère les appels REST vers un autre microservice.

## 9. Pourquoi ne pas partager la base Customer ?

Chaque microservice possède ses données. Partager la base crée un couplage fort et casse l’autonomie (déploiements, schémas, ownership).

## 10. Pourquoi `@Transient` sur `customer` ?

Le client enrichi n’est **pas** une colonne JPA. On persiste seulement `customerId` ; l’objet `Customer` est ajouté en mémoire pour la réponse JSON.

## 11. Qu’est-ce que Resilience4J ?

Une bibliothèque de résilience : Circuit Breaker, retry, etc. Ici on utilise le **Circuit Breaker** pour protéger les appels Feign vers Customer.

## 12. CLOSED / OPEN / HALF_OPEN ?

- **CLOSED** : appels normaux.
- **OPEN** : trop d’échecs → appels court-circuités (fallback).
- **HALF_OPEN** : après un délai, quelques appels tests pour tenter la fermeture.

## 13. Pourquoi le fallback de lecture retourne `null` ?

Pour garder les comptes lisibles sans inventer un client. `customer=null` signale clairement l’indisponibilité de l’enrichissement.

## 14. Pourquoi POST reste strict ?

Créer un compte sans valider le client produirait des données incohérentes. Le fallback de création **refuse** toujours.

## 15. Pourquoi H2 ?

Base légère en mémoire, parfaite pour un TP local : pas d’installation SGBD, démarrage rapide. Inconvénient : perte des données au stop du process.

## 16. Que se passe-t-il après redémarrage d’un service ?

H2 mémoire est vide puis re-seedée (clients 1–3, 15 comptes). Les clients créés dynamiquement avant le stop **disparaissent**.

## 17. Comment vérifier les routes Gateway ?

`GET http://localhost:9999/actuator/gateway/routes` ou tester `/CUSTOMER-SERVICE/...` et `/EBANK-SERVICE/...`.

## 18. Différence tests unitaires / intégration ?

- **Maven/Spring** : tests automatisés dans le module (mocks, contexte).
- **Intégration E2E** : vrais processus, vrais ports HTTP, panne réelle (Phase 7).

## 19. Pourquoi le HTTP 500 peut être problématique ?

Pour un « client introuvable », un **404** serait plus REST. Ici Customer renvoie 500 : le client Feign doit distinguer métier vs panne (best-effort via message).

## 20. Quelles améliorations pour la production ?

PostgreSQL, Docker, sécurité (OAuth2/Keycloak), Config Server, monitoring (Prometheus/Grafana), tracing (Zipkin/Tempo), file d’événements (Kafka), HA Eureka, etc. — **non implémentés** dans ce TP.

## 21. Pourquoi démarrer Eureka en premier ?

Sans registre, Gateway et Feign ne peuvent pas résoudre correctement les noms de services.

## 22. Qu’est-ce que le discovery locator ?

Fonctionnalité Gateway qui crée automatiquement des routes `/{serviceId}/**` à partir des applications Eureka.

## 23. Comment observe-t-on le Circuit Breaker ?

Endpoint Actuator `/actuator/circuitbreakers` sur EBank : état, failureRate, notPermittedCalls.

## 24. Pourquoi un bean `CustomerLookupService` séparé ?

Pour que l’annotation `@CircuitBreaker` soit interceptée par le proxy AOP. Un appel interne (`this.method`) contournerait le proxy.

## 25. Les bases H2 sont-elles partagées ?

Non : `customers-db` et `accounts-db` sont indépendantes.

## 26. Que signifie Phase 7 « 30 PASS » ?

Trente scénarios de la matrice d’intégration (Maven inclus dans la matrice + HTTP + résilience + config) tous validés — voir `logs/phase7/final-test-matrix.md`.

## 27. Combien de tests Maven ?

**25** : Customer 4 + EBank 17 + Gateway 3 + Discovery 1.

## 28. Kafka est-il dans ce projet ?

Non. Le TP4 Kafka est hors périmètre de cette Partie 1.

## 29. Que montre Swagger ?

La documentation interactive OpenAPI des endpoints Customer et EBank (pas un remplacement des tests).

## 30. Que faire si Gateway répond 404 juste après le démarrage ?

Attendre la propagation Eureka / le cache du discovery locator, puis réessayer. Observé en Phase 7 comme comportement transitoire.
