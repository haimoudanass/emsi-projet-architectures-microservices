# Guide de démarrage Windows

Chemins relatifs au dépôt. Java 21 n’est **pas** supposé être dans le PATH global.

## 1. Ouvrir PowerShell

```powershell
cd <racine-du-depot>   # dossier contenant pom.xml et scripts/
```

## 2. Charger l’environnement de session

```powershell
# Option recommandée : définir JAVA_HOME / MAVEN_HOME si besoin, puis :
. .\scripts\init-env.ps1
```

Exemple (session uniquement) :

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot"
$env:MAVEN_HOME = "C:\Path\To\apache-maven-3.9.6"
. .\scripts\init-env.ps1
```

Ce script fixe `JAVA_HOME` / `MAVEN_HOME` **uniquement pour la session courante**.

## 3. Vérifier Java et Maven

```powershell
java -version
mvn -version
```

Attendu : Java 21, Maven 3.9.x.

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
. .\scripts\init-env.ps1
java -jar .\discovery-service\target\discovery-service-1.0.0-SNAPSHOT.jar
```

**Fenêtre 2 — Customer** (après Eureka prêt)

```powershell
. .\scripts\init-env.ps1
java -jar .\customer-service\target\customer-service-1.0.0-SNAPSHOT.jar
```

**Fenêtre 3 — EBank**

```powershell
. .\scripts\init-env.ps1
java -jar .\ebank-service\target\ebank-service-1.0.0-SNAPSHOT.jar
```

**Fenêtre 4 — Gateway**

```powershell
. .\scripts\init-env.ps1
java -jar .\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar
```

## 10. Attendre Eureka UP

Navigateur : http://localhost:8761/

Ou :

```powershell
curl http://localhost:8761/eureka/apps
```

Vérifier `CUSTOMER-SERVICE`, `EBANK-SERVICE`, `GATEWAY-SERVICE` en statut **UP**.

## 11. Smoke HTTP

```text
GET http://localhost:9999/CUSTOMER-SERVICE/customers
GET http://localhost:9999/EBANK-SERVICE/accounts
```

Swagger :

- http://localhost:8056/swagger-ui/index.html
- http://localhost:8057/swagger-ui/index.html

## 12. Arrêt

Arrêter chaque processus Java (Ctrl+C) dans l’ordre inverse : Gateway → EBank → Customer → Discovery.
