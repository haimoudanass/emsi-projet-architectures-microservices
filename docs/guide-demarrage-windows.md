# Guide de démarrage Windows

Chemins réels du projet. Java 21 n’est **pas** supposé être dans le PATH global.

## 1. Ouvrir PowerShell

```powershell
cd C:\Users\Administrator\Desktop\microservices-project
```

## 2. Charger l’environnement de session

```powershell
. "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"
```

Ce script fixe `JAVA_HOME` / `MAVEN_HOME` **uniquement pour la session courante**.

## 3. Vérifier Java et Maven

```powershell
java -version
mvn -version
```

Attendu : Java 21, Maven 3.9.6.

## 4. Vérifier les ports libres

```powershell
netstat -ano | findstr ":8056 :8057 :8761 :9999" | findstr "LISTENING"
```

Aucun résultat = ports libres.

## 5. Build

```powershell
mvn clean verify
```

Attendu : `BUILD SUCCESS`, 25 tests (4+17+3+1).

## 6–9. Démarrer les services (4 fenêtres PowerShell)

Dans **chaque** fenêtre : recharger `init-env.ps1`, puis :

**Fenêtre 1 — Discovery**

```powershell
. "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"
java -jar "C:\Users\Administrator\Desktop\microservices-project\discovery-service\target\discovery-service-1.0.0-SNAPSHOT.jar"
```

**Fenêtre 2 — Customer** (après Eureka prêt)

```powershell
. "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"
java -jar "C:\Users\Administrator\Desktop\microservices-project\customer-service\target\customer-service-1.0.0-SNAPSHOT.jar"
```

**Fenêtre 3 — EBank**

```powershell
. "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"
java -jar "C:\Users\Administrator\Desktop\microservices-project\ebank-service\target\ebank-service-1.0.0-SNAPSHOT.jar"
```

**Fenêtre 4 — Gateway**

```powershell
. "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"
java -jar "C:\Users\Administrator\Desktop\microservices-project\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar"
```

## 10. Attendre Eureka UP

Navigateur : http://localhost:8761/

Ou :

```powershell
Invoke-RestMethod "http://localhost:8761/eureka/apps" -Headers @{Accept='application/json'}
```

Vérifier `CUSTOMER-SERVICE`, `EBANK-SERVICE`, `GATEWAY-SERVICE` en statut **UP**.
Compter **10 à 60 secondes** après démarrage.

Si Gateway renvoie 404 sur `/CUSTOMER-SERVICE/customers`, patienter quelques secondes (cache discovery locator) puis retester.

## 11. Tester les endpoints

```powershell
Invoke-WebRequest http://localhost:8056/customers -UseBasicParsing
Invoke-WebRequest http://localhost:9999/CUSTOMER-SERVICE/customers -UseBasicParsing
Invoke-WebRequest http://localhost:8057/accounts -UseBasicParsing
Invoke-WebRequest http://localhost:9999/EBANK-SERVICE/accounts -UseBasicParsing
```

## 12. Swagger

- http://localhost:8056/swagger-ui/index.html
- http://localhost:8057/swagger-ui/index.html

## 13. Arrêt propre

Dans chaque fenêtre de service : `Ctrl+C`.

Vérifier :

```powershell
netstat -ano | findstr ":8056 :8057 :8761 :9999" | findstr "LISTENING"
```

## Automatisation Phase 7 (optionnel)

```powershell
. .\scripts\init-env.ps1
mvn clean verify
powershell -ExecutionPolicy Bypass -File .\scripts\phase7\run-integration-tests.ps1
```

Le script démarre les jars, exécute les tests HTTP, puis n’arrête que les processus qu’il a créés.
