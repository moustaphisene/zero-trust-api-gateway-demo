# 🏛️ Décisions d'architecture

## Vue d'ensemble

Architecture issue du **TP8**: un Gateway qui valide le token, et des
microservices qui **re-valident** — la sécurité n'est jamais déléguée à une seule couche.

## Flux d'authentification (OAuth2 / OIDC — réf. 2.5, 9.2)

```
1. Client ──(username/password, client_id, client_secret)──► Keycloak /token
2. Keycloak ──(access_token JWT signé RS256)──► Client
3. Client ──(Authorization: Bearer <JWT>)──► Gateway /api/tenders
4. Gateway : valide JWT (JWKS) → applique rate limit → propage X-User-* → route
5. tender-service : RE-valide JWT (JWKS) → RBAC (@PreAuthorize) → ABAC (PermissionEvaluator)
6. tender-service ──(réponse)──► Gateway ──► Client
```

## Choix techniques notables

| Décision | Justification |
|----------|---------------|
| **Validation JWT par `jwk-set-uri`** (et non `issuer-uri`) | Évite le conflit d'issuer entre l'hôte (`localhost:8180`) et le réseau Docker (`keycloak:8080`). La signature RS256 et l'expiration sont validées. En prod : ajouter la validation d'issuer avec un hostname Keycloak fixe. |
| **Rôles composites Keycloak** | ADMIN ⊃ MANAGER ⊃ USER ; mappent les permissions fines (`tender:*`) sans les attribuer une à une. |
| **Keycloak en `start-dev` (H2)** | Démarrage simple, sans couplage à Postgres. Pour la prod : base externe + `start` + hostname. |
| **Vault `fail-fast: false` + clé de repli** | La démo démarre même si Vault est indisponible ; en prod, la clé vient **uniquement** de Vault. |
| **Conteneurs non-root** | Durcissement (Annexe A) : chaque image applicative tourne sous l'utilisateur `app`. |
| **DTO ≠ Entité** | Empêche le *mass assignment* (OWASP A03) ; `tenantId`/`createdBy`/`status` sont imposés serveur. |
| **404 au lieu de 403 sur cross-tenant** | Anti-énumération : ne révèle pas l'existence d'une ressource d'un autre tenant (BOLA / A01). |

## Ports

| Port | Service |
|------|---------|
| 8080 | Gateway (HTTP) / 8443 (HTTPS profil `https`) |
| 8081 | tender-service |
| 8082 | user-service |
| 8180 | Keycloak |
| 8200 | Vault |
| 5432 | PostgreSQL |
| 6379 | Redis |
| 9090 | Prometheus |
| 3000 | Grafana |