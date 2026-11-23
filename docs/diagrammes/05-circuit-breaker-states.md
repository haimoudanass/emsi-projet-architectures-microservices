# Diagramme — États du Circuit Breaker

Instance : `customerService`

```mermaid
stateDiagram-v2
  [*] --> CLOSED

  CLOSED --> OPEN: Taux echecs >= 50%\napres minimum 3 appels\n(fenetre COUNT_BASED 5)
  OPEN --> HALF_OPEN: waitDuration 10s\n(automaticTransition)
  HALF_OPEN --> CLOSED: 2 appels permis reussis
  HALF_OPEN --> OPEN: Echec pendant HALF_OPEN

  CLOSED: Appels Feign normaux\nEnrichissement OK
  OPEN: Appels court-circuites\nfallback lecture null\nPOST refuse
  HALF_OPEN: 2 appels de test\nvers Customer
```

## Preuves Phase 6 / 7 (Actuator)

| État | Observation typique |
|------|---------------------|
| CLOSED | `failureRate=0%` en fonctionnement normal |
| OPEN | `notPermittedCalls` > 0, lectures via fallback |
| HALF_OPEN | Après ~10 s d’attente OPEN |
| CLOSED | Après 2 succès en HALF_OPEN |

Consulter : `GET http://localhost:8057/actuator/circuitbreakers`
