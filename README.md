# 🔐 Sécurisation des API REST avec Spring Boot — Démo prête à l'emploi

Projet de démonstration complet illustrant les pratiques de sécurité des API REST,
d'après mes expériences professionnelles RETEX, mes lectures et recherches (2025-2026)).

Architecture **microservices Zero Trust** : une **API Gateway** sécurisée protège deux
microservices ressources (`tender-service`, `user-service`), avec **Keycloak** comme
Identity Provider, **HashiCorp Vault** pour les secrets, et une stack **Prometheus/Grafana**
pour l'observabilité de la sécurité.

> 👤 Auteur de la démo : **Moustapha SENE** — NFS GROUP ex SEN IT SERVICES PRO.

---

## 🏗️ Architecture

```
                         ┌──────────────────────────┐
   Postman / Client ───► │   API GATEWAY  :8080      │  En-têtes sécu 
        (JWT)            │  Spring Cloud Gateway     │  Validation JWT 
                         │                           │  Rate limiting 
                         └───┬──────────────┬────────┘  Propagation identité (Zero Trust)
                             │ JWT          │ JWT
                   ┌─────────▼───┐    ┌─────▼─────────┐
                   │ tender-svc  │    │  user-svc     │   Re-validation JWT 
                   │ :8081       │    │  :8082        │   RBAC + ABAC 
                   │ RBAC+ABAC   │    └───────────────┘
                   │ AES, Vault  │
                   └─────┬───────┘
              ┌──────────┼──────────┬───────────┬──────────┐
        ┌─────▼────┐ ┌───▼───┐ ┌────▼────┐ ┌────▼─────┐ ┌──▼──────┐
        │ Keycloak │ │ Vault │ │ Postgres│ │  Redis   │ │Prom+Graf│
        │  :8180   │ │ :8200 │ │  :5432  │ │  :6379   │ │9090/3000│
        └──────────┘ └───────┘ └─────────┘ └──────────┘ └─────────┘
```

---

## 🚀 Démarrage rapide

**Pré-requis :** Docker + Docker Compose. *(Aucun JDK 21 requis sur l'hôte : les services
sont construits dans les conteneurs.)*

```bash
# 1) Lancer toute la stack
make up           # ou : docker compose up -d --build

# 2) Attendre ~1-2 min que Keycloak et les services soient UP
make ps

# 3) Test de fumée (token → Gateway → RBAC)
make smoke
```

| Service     | URL                          | Identifiants        |
|-------------|------------------------------|---------------------|
| Gateway     | http://localhost:8080        | —                   |
| Keycloak    | http://localhost:8180        | `admin` / `admin`   |
| Vault       | http://localhost:8200        | token `root`        |
| Prometheus  | http://localhost:9090        | —                   |
| Grafana     | http://localhost:3000        | `admin` / `admin`   |

---

## 📮 Collection Postman

Importer dans Postman :

1. `postman/API-Security-Course.postman_collection.json`
2. `postman/API-Security-Course.postman_environment.json` (sélectionner l'environnement *Local*)

Puis exécuter **dans l'ordre** :
- **Dossier 0 — Authentification** : récupère un token pour chaque rôle (admin/manager/user/auditor).
- **Dossiers 1 à 5** : RBAC/ABAC, scénarios OWASP/Zero Trust, utilisateurs, rate limiting, observabilité.

> Astuce : *Collection Runner* sur le dossier 4 (×30 itérations) pour déclencher un `429` (rate limit).

---

## 👥 Comptes de démonstration (realm `api-realm`)

| Utilisateur | Mot de passe | Rôle    | Département  | Plafond d'approbation | Tenant       |
|-------------|--------------|---------|--------------|-----------------------|--------------|
| `admin`     | `admin123`   | ADMIN   | IT           | 10 000 000            | tenant-alpha |
| `manager`   | `manager123` | MANAGER | ENGINEERING  | 500 000               | tenant-alpha |
| `user`      | `user123`    | USER    | FINANCE      | 0                     | tenant-alpha |
| `auditor`   | `auditor123` | AUDITOR | COMPLIANCE   | 0                     | tenant-alpha |

Matrice RBAC appliquée dans `TenderController` ; politiques ABAC dans
`AbacPermissionEvaluator` : *même département*, *montant ≤ plafond*, *heures ouvrables*.

---

## 🔒 HTTPS / TLS (optionnel)

```bash
make up-https     # génère le keystore auto-signé puis démarre le Gateway en TLS (8443)
```

---

## 🛡️ mTLS inter-services (optionnel)

TLS mutuel entre le Gateway et les microservices : chaque appel interne est chiffré et
**authentifié des deux côtés** par certificat X.509 (chiffrement bout-en-bout du Zero Trust).

```bash
make up-mtls      # génère la PKI puis démarre la stack avec mTLS Gateway ↔ services
```

Preuve d'application : un appel direct sans certificat client est **rejeté au niveau TLS**.

```bash
curl -k https://localhost:8081/api/tenders   # ⇒ échec handshake (certificat client requis)
```

Détails et activation du mTLS : **[docs/ZERO-TRUST-MTLS.md](docs/ZERO-TRUST-MTLS.md)**.

---

## 🔑 Credentials PostgreSQL dynamiques (optionnel)

Avec le moteur **Database de Vault**, le tender-service n'a plus de mot de passe statique :
Vault lui délivre un **compte PostgreSQL éphémère** (TTL court, révoqué automatiquement).

```bash
make up-vaultdb        # démarre la stack avec credentials DB dynamiques
make vaultdb-whoami    # montre le compte éphémère v-token-… utilisé par le service
```

Astuce d'ownership : chaque compte dynamique est créé `IN ROLE tender_user` avec
`ALTER ROLE … SET ROLE tender_user`, donc il ne possède aucun objet et reste révocable
proprement. Détails : **[docs/VAULT-DYNAMIC-DB.md](docs/VAULT-DYNAMIC-DB.md)**.

---

## 🧭 Où trouver quoi ?

Voir **[docs/THEMES-MAPPING.md](docs/THEMES-MAPPING.md)** : chaque thème couvert est
relié au fichier qui l'implémente. Voir aussi :
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — décisions d'architecture et flux ;
- **[docs/ZERO-TRUST-MTLS.md](docs/ZERO-TRUST-MTLS.md)** — Zero Trust et activation du mTLS ;
- **[docs/SECURITY-CHECKLIST.md](docs/SECURITY-CHECKLIST.md)** — check-list de sécurité ;
- **[docs/COMPLIANCE.md](docs/COMPLIANCE.md)** — cartographie contrôles → ISO 27001 / RGPD / OWASP ;
- **[docs/SUPPLY-CHAIN.md](docs/SUPPLY-CHAIN.md)** — SBOM, signature d'images, provenance.

---

## 🧪 Tests automatisés

```bash
# Tests unitaires + slice MVC (rapides, sans Docker)
mvn -pl tender-service test

# + tests d'intégration Testcontainers (PostgreSQL réel) — nécessite Docker
mvn -pl tender-service verify
```

| Type | Classe | Couvre |
|------|--------|--------|
| Unitaire | `AesEncryptionServiceTest` | AES-256-GCM : round-trip, IV aléatoire, détection d'altération |
| Unitaire | `AbacPermissionEvaluatorTest` | Politiques ABAC (même département, plafond d'approbation) |
| Slice MVC | `TenderControllerSecurityTest` | RBAC (@PreAuthorize), ABAC, BOLA — via `jwt()` simulé |
| Intégration | `TenderPersistenceIT` | PostgreSQL réel : persistance, RBAC bout-en-bout, **AES au repos** |

La CI (`.github/workflows/ci-secure.yml`) exécute `mvn verify` + SAST (CodeQL) + SCA/image-scan (Trivy) + secret-scan (Gitleaks).

---

## 🧩 Stack technique

Spring Boot 3.3 · Spring Cloud Gateway 2023.0 · Spring Security (Resource Server OAuth2) ·
Keycloak 25 · HashiCorp Vault 1.17 · PostgreSQL 16 · Redis 7 · Micrometer/Prometheus · Grafana 11.

## ⚠️ Avertissement

Toutes les valeurs (mots de passe, tokens Vault, secrets clients, keystore auto-signé) sont
des **valeurs de démonstration**. Ne jamais les utiliser en production.