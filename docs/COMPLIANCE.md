# 🧭 Cartographie de conformité — contrôles → référentiels

Ce document relie chaque mesure de sécurité **implémentée dans le code** à un ou plusieurs
**référentiels** : ISO/IEC 27001:2022 (Annexe A), RGPD, OWASP API Security Top 10 (2023).
Objectif : passer de « du code sécurisé » à « des **contrôles traçables et auditables** ».

> **Positionnement gouvernance.** Cette traçabilité est le maillon entre l'architecture
> (TOGAF — couche sécurité de l'EA), l'analyse de risque (**EBIOS RM** / ISO 27005) et le
> Système de Management de la Sécurité (**ISO 27001**). Les contrôles ci-dessous sont les
> *mesures de traitement du risque* ; le code en est la preuve de mise en œuvre.

---

## 1. Contrôles techniques → référentiels

| Mesure (implémentée) | ISO 27001:2022 (Annexe A) | RGPD | OWASP API | Implémentation |
|----------------------|---------------------------|------|-----------|----------------|
| Authentification centralisée (OIDC/JWT, IdP) | A.8.5 Authentification sécurisée ; A.5.17 Informations d'authentification | Art. 32 | API2 | Keycloak `realm-export.json` ; Resource Server (`SecurityConfig`) |
| Contrôle d'accès RBAC | A.5.15 Contrôle d'accès ; A.8.2 Droits d'accès à privilèges ; A.5.18 Droits d'accès | Art. 25, 32 | API1, API5 | `@PreAuthorize` (`TenderController`, `UserController`), `KeycloakRealmRoleConverter` |
| Contrôle d'accès ABAC (attributs, contexte) | A.5.15 ; A.8.3 Restriction d'accès à l'information | Art. 5(1)(c) minimisation | API1, API5 | `AbacPermissionEvaluator`, `MethodSecurityConfig` |
| Isolation multi-tenant (anti-BOLA) | A.8.3 ; A.8.11 Masquage des données | Art. 5(1)(f) ; Art. 32 | API1 | `TenderController.loadWithTenantCheck` |
| Validation des entrées | A.8.28 Codage sécurisé | — | API3, API8 | `TenderDtos` (Bean Validation), `GlobalExceptionHandler` |
| Anti-injection (requêtes paramétrées) | A.8.28 | Art. 32 | API8 | `TenderRepository` (JPQL paramétré) |
| Chiffrement en transit (TLS) | A.8.24 Cryptographie ; A.8.20 Sécurité des réseaux | Art. 32(1)(a) | — | `application-https.yml`, profil `https` |
| **mTLS** inter-services | A.8.24 ; A.8.20 ; A.8.21 Sécurité des services réseau | Art. 32 | — | `application-mtls.yml`, `docker-compose.mtls.yml`, `docs/ZERO-TRUST-MTLS.md` |
| Chiffrement au repos (AES-256-GCM) | A.8.24 ; A.8.11 | Art. 32(1)(a) | — | `AesEncryptionService`, `AesAttributeConverter` |
| Gestion des secrets (Vault) | A.5.17 ; A.8.24 | Art. 32 | API8 | `infra/vault/bootstrap.sh`, `spring.cloud.vault` |
| Credentials DB **dynamiques** (TTL court) | A.8.2 ; A.5.17 ; A.8.3 | Art. 32 | API8 | `configure-db-engine.sh`, profil `vaultdb`, `docs/VAULT-DYNAMIC-DB.md` |
| En-têtes HTTP de sécurité (CSP, HSTS…) | A.8.26 Exigences de sécurité applicative ; A.8.23 Filtrage web | — | API8 | `SecurityConfig` (3 services) |
| Rate limiting / anti-DDoS | A.8.6 Gestion des capacités ; A.8.16 Surveillance | — | API4 | `RateLimiterConfig` + `RequestRateLimiter` (Redis) |
| Zero Trust (deny-by-default, re-validation) | A.8.27 Architecture sécurisée ; A.5.15 | Art. 25 by design | API1, API5 | `SecurityConfig` (deny-by-default), filtre JWT, anti header-spoofing |
| Protection brute-force (verrouillage) | A.8.5 ; A.5.15 | Art. 32 | API2 | Keycloak `bruteForceProtected` |
| Durée de vie des tokens / rotation des clés | A.8.24 ; A.5.17 | Art. 32 | API2 | Keycloak realm (RS256, JWKS), `accessTokenLifespan` |
| Journalisation d'audit (SIEM-ready) | A.8.15 Journalisation ; A.5.28 Collecte de preuves | Art. 33/34 (notification de violation) | API8 | `SecurityEventLogger` (logger `SECURITY_AUDIT`) |
| Surveillance & alertes | A.8.16 Surveillance des activités | Art. 32(1)(d) | API4 | `SecurityMetrics`, `infra/prometheus/alert-rules.yml`, Grafana |
| Gestion des vulnérabilités (SCA) | A.8.8 Gestion des vulnérabilités techniques | Art. 32 | API8 | CI : Trivy (fs + image) |
| Analyse statique (SAST) | A.8.28 ; A.8.29 Tests de sécurité | — | API8 | CI : CodeQL |
| Détection de secrets | A.5.17 ; A.8.4 Accès au code source | — | — | CI : Gitleaks |
| **SBOM** & traçabilité des composants | A.8.8 ; A.5.21 Sécurité de la chaîne d'approvisionnement TIC | — | API9, API10 | CI : CycloneDX (`docs/SUPPLY-CHAIN.md`) |
| **Signature d'images** (intégrité/provenance) | A.5.23 Services cloud ; A.8.31 Séparation des environnements | — | API10 | CI : cosign keyless (`docs/SUPPLY-CHAIN.md`) |
| Conteneurs non-root | A.8.27 ; A.8.9 Gestion de configuration | — | API8 | `Dockerfile` (user `app`) |
| Tests de sécurité automatisés | A.8.29 ; A.8.25 Cycle de dev sécurisé | — | — | `*Test`, `*IT` (RBAC/ABAC/BOLA, AES) |
| Séparation dev / test / prod & secrets | A.8.31 ; A.8.9 | Art. 32 | API8 | profils Spring, `.env.example`, `.gitignore` |

---

## 2. Couverture OWASP API Security Top 10 (2023)

| Risque | Statut | Où |
|--------|--------|-----|
| API1 — Broken Object Level Authorization | ✅ | isolation tenant (`loadWithTenantCheck`), 404 anti-énumération |
| API2 — Broken Authentication | ✅ | Keycloak (OIDC, brute-force, RS256), validation JWT |
| API3 — Broken Object Property Level Authorization | ✅ | DTO ≠ entité, anti mass-assignment (`TenderDtos`) |
| API4 — Unrestricted Resource Consumption | ✅ | rate limiting Redis, timeouts httpclient |
| API5 — Broken Function Level Authorization | ✅ | RBAC `@PreAuthorize` par fonction + permissions fines |
| API6 — Unrestricted Access to Sensitive Business Flows | ➖ | ABAC contextuel (horaires/plafond) ; à étendre selon métier |
| API7 — Server Side Request Forgery | ➖ | pas d'appel sortant piloté par l'utilisateur dans la démo |
| API8 — Security Misconfiguration | ✅ | en-têtes, pas de stack trace, durcissement, scans CI |
| API9 — Improper Inventory Management | ✅ | SBOM, versions épinglées, doc d'architecture |
| API10 — Unsafe Consumption of APIs | ✅ | mTLS + validation des pairs ; SBOM/signature sur les images |

---

## 3. Lien avec EBIOS RM / ISO 27005

Les contrôles ci-dessus constituent les **mesures de sécurité** (traitement du risque) face
aux scénarios redoutés typiques d'une API métier :

| Événement redouté | Source de risque | Mesure(s) de traitement |
|-------------------|------------------|--------------------------|
| Accès non autorisé aux données d'un autre client | attaquant authentifié | RBAC + ABAC + isolation tenant (API1/API5) |
| Vol / fuite de credentials | attaquant externe / interne | Vault, creds dynamiques TTL court, secrets non committés |
| Interception du trafic interne | attaquant sur le réseau | mTLS, chiffrement au repos |
| Déni de service | botnet | rate limiting, timeouts, surveillance |
| Compromission de la chaîne de build | dépendance/​image malveillante | SCA, SAST, SBOM, signature d'images |

---

## 4. Risques résiduels / écart démo → production

Honnêteté d'architecte : cette démo n'est **pas** prête pour la production en l'état.

- Secrets et mots de passe = **valeurs de démonstration** (clé AES stable, token Vault `root`).
- CA mTLS **auto-signée** ; en prod : CA d'entreprise / SPIFFE-SPIRE + rotation.
- Vault en **mode dev** (in-memory) ; en prod : HA, stockage scellé, auth AppRole/Kubernetes.
- Keycloak en **H2 dev** ; en prod : base externe, hostname fixe, HTTPS.
- Pas encore de **WAF**, de gestion centralisée des logs (SIEM réel), ni de **HA / PRA-PCA**.

Le détail de durcissement figure dans chaque doc de profil (`ZERO-TRUST-MTLS.md`,
`VAULT-DYNAMIC-DB.md`) et dans `SECURITY-CHECKLIST.md`.