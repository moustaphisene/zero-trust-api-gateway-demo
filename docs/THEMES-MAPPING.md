# 🗺️ Cartographie thèmes → implémentation

Chaque thème demandé est relié au(x) fichier(s) qui l'implémente(nt) dans la démo.

| # | Thème du programme | Implémentation |
|---|--------------------|----------------|
| 1.2.3 | **En-têtes HTTP de sécurité** (CSP, HSTS, X-Frame-Options, nosniff…) | `gateway/.../config/SecurityConfig.java`, `tender-service/.../config/SecurityConfig.java`, `user-service/.../config/SecurityConfig.java` |
| 1.3.2 / 1.3.3 | **HTTPS sur TLS** | `gateway/.../application-https.yml`, `infra/certs/generate-certs.sh`, `make up-https` |
| 1.4 | **JSON Web Token (JWT)** | Resource Server OAuth2 dans les 3 `SecurityConfig` ; émission par Keycloak (`infra/keycloak/realm-export.json`) |
| **Module 1** | **Sécurité des API REST** | Ensemble `tender-service` + `gateway` |
| 2.3 | **Identity Provider & Keycloak** | `infra/keycloak/realm-export.json` (realm `api-realm`, clients, users, mappers) |
| 2.4 | **RBAC et ABAC** | RBAC : rôles composites Keycloak + `@PreAuthorize` (`TenderController`, `KeycloakRealmRoleConverter`). ABAC : `AbacPermissionEvaluator` + `MethodSecurityConfig` |
| 2.6 | **API Gateway Pattern** | `gateway/` (Spring Cloud Gateway), routes dans `application.yml` |
| 2.6.2 | **Responsabilités sécuritaires de la Gateway** | `gateway/.../config/SecurityConfig.java` (en-têtes, deny-by-default), filtre JWT, rate limiter |
| 2.6.4 | **Filtre de validation JWT personnalisé** | `gateway/.../filter/JwtValidationGatewayFilterFactory.java` |
| 2.6.5 | **Rate Limiting & protection DDoS** | `gateway/.../config/RateLimiterConfig.java` + `RequestRateLimiter` (Redis) dans `application.yml` |
| **Module 2** | **Gestion des tokens et clés API** | Signature RS256 via JWKS (`jwk-set-uri`), rotation des clés Keycloak, durée de vie des access tokens (realm) |
| **Module 3** | **OWASP Top 10 pour les API** | A01/BOLA & multi-tenant (`TenderController.loadWithTenantCheck`), A03/Injection (`TenderRepository`, requêtes paramétrées + Bean Validation), A03/Mass-assignment (`TenderDtos`), A05/Misconfig (`GlobalExceptionHandler`, en-têtes), A07/Auth (Keycloak brute-force) |
| **Module 8** | **Sécurité Gateway & microservices** | Architecture TP8 : Gateway + 2 services, re-validation JWT côté service, propagation d'identité, anti header-spoofing |
| 9.5 | **Zero Trust** | Deny-by-default + re-validation JWT par chaque service + suppression des en-têtes X-* clients ; voir `docs/ZERO-TRUST-MTLS.md` |
| 10.2 | **Gestion des secrets — HashiCorp Vault** | `infra/vault/bootstrap.sh`, config `spring.cloud.vault` (`tender-service/application.yml`), clé AES servie par Vault |
| 5.3 | **Chiffrement au repos (AES-256-GCM)** | `tender-service/.../crypto/AesEncryptionService.java` + `AesAttributeConverter.java` |
| CI/CD | **Pipeline CI/CD sécurisé** | `.github/workflows/ci-secure.yml` (build, SAST CodeQL, SCA Trivy, secret-scan Gitleaks, image-scan) |
| **Annexe G** | **Monitoring & observabilité de la sécurité** | `SecurityEventLogger` + `SecurityMetrics` (gateway & tender), `infra/prometheus/*`, `infra/grafana/*` |

## Démonstrations clés (via Postman)

| Concept | Requête Postman |
|---------|-----------------|
| RBAC — création réservée MANAGER/ADMIN | `1 → Créer une offre (MANAGER)` vs `… REFUSÉ (USER 403)` |
| ABAC — même département | `1 → Modifier offre — ABAC même département` vs `… autre département (403)` |
| ABAC — plafond & horaires | `1 → Approuver offre — ABAC` |
| BOLA / A01 — isolation tenant | `2 → BOLA — accès cross-tenant (404)` |
| Zero Trust — anti header-spoofing | `2 → Header spoofing — X-User-Roles injecté` |
| Zero Trust — re-validation hors Gateway | `2 → Accès direct au microservice (401)` |
| Rate limiting | `4 → Rafale (Runner ×30) → 429` |
| Observabilité | `5 → métriques Prometheus` |