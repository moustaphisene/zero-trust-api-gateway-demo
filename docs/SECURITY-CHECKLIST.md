# ✅ Check-list de sécurité

État de la démo vis-à-vis des bonnes pratiques. ✔ = implémenté, ➖ = documenté/à activer en prod.

## Transport & en-têtes
- ✔ En-têtes de sécurité (CSP, HSTS, X-Frame-Options DENY, nosniff, Referrer-Policy) — `SecurityConfig`
- ➖ HTTPS/TLS 1.2-1.3 au edge — profil `https` (`make up-https`)
- ➖ mTLS entre services — `docs/ZERO-TRUST-MTLS.md`
- ✔ Cache-Control `no-store` sur les routes `/api/**`

## Authentification & autorisation
- ✔ JWT signés RS256, validés via JWKS (signature + expiration)
- ✔ RBAC (rôles composites Keycloak + `@PreAuthorize`)
- ✔ ABAC (PermissionEvaluator : département, plafond, horaires)
- ✔ Permissions fines (`tender:read/write/delete`, `user:manage`)
- ✔ Protection brute-force Keycloak (`bruteForceProtected`)
- ✔ Sessions stateless (pas de cookie de session)

## OWASP API Top 10
- ✔ A01 BOLA — isolation multi-tenant + 404 anti-énumération
- ✔ A02 Broken Auth — délégué à Keycloak, lifespan court (900 s)
- ✔ A03 Property-level / Mass assignment — DTO dédiés
- ✔ A03 Injection — requêtes JPA paramétrées, Bean Validation
- ✔ A05 Misconfiguration — `GlobalExceptionHandler` (pas de stack trace), header `Server` retiré
- ✔ A04/Rate limiting — `RequestRateLimiter` Redis

## Secrets & données
- ✔ Secrets via Vault (clé AES, secret JWT)
- ✔ Chiffrement au repos AES-256-GCM (données scellées)
- ✔ Aucun secret en dur committé (`.gitignore`, `.env.example`)
- ✔ Credentials DB dynamiques (Vault Database engine) — profil `vaultdb`, comptes PostgreSQL éphémères (`make up-vaultdb`)

## CI/CD & exploitation
- ✔ Pipeline DevSecOps : SAST (CodeQL), SCA (Trivy), secret-scan (Gitleaks), image-scan
- ✔ Images Docker non-root
- ✔ Observabilité : logs `SECURITY_AUDIT`, métriques Prometheus, alertes, dashboard Grafana
- ✔ Sondes de santé (`/actuator/health`)