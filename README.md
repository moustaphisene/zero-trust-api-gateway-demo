# 🔐 Sécurisation des API REST avec Spring Boot — Démo prête à l'emploi

Projet de démonstration complet illustrant les pratiques de sécurité des API REST,
d'après l'ouvrage *Sécurisation des API REST avec Spring Boot* (Dr. Badr El Khalyly, 2025-2026).

Architecture **microservices Zero Trust** : une **API Gateway** sécurisée protège deux
microservices ressources (`tender-service`, `user-service`), avec **Keycloak** comme
Identity Provider, **HashiCorp Vault** pour les secrets, et une stack **Prometheus/Grafana**
pour l'observabilité de la sécurité.

> 👤 Auteur de la démo : **Moustapha SENE** — SEN IT SERVICES PRO.

---

## 🏗️ Architecture

```
                         ┌──────────────────────────┐
   Postman / Client ───► │   API GATEWAY  :8080      │  En-têtes sécu (1.2.3)
        (JWT)            │  Spring Cloud Gateway     │  Validation JWT (2.6.4)
                         │                           │  Rate limiting (2.6.5)
                         └───┬──────────────┬────────┘  Propagation identité (Zero Trust)
                             │ JWT          │ JWT
                   ┌─────────▼───┐    ┌─────▼─────────┐
                   │ tender-svc  │    │  user-svc     │   Re-validation JWT (9.5)
                   │ :8081       │    │  :8082        │   RBAC + ABAC (2.4)
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

Matrice RBAC (Table 2.4) appliquée dans `TenderController` ; politiques ABAC (2.4.2) dans
`AbacPermissionEvaluator` : *même département*, *montant ≤ plafond*, *heures ouvrables*.

---

## 🔒 HTTPS / TLS (optionnel — réf. 1.3.3)

```bash
make up-https     # génère le keystore auto-signé puis démarre le Gateway en TLS (8443)
```

---

## 🧭 Où trouver quoi ?

Voir **[docs/THEMES-MAPPING.md](docs/THEMES-MAPPING.md)** : chaque thème du programme est
relié au fichier qui l'implémente. Voir aussi :
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — décisions d'architecture et flux ;
- **[docs/ZERO-TRUST-MTLS.md](docs/ZERO-TRUST-MTLS.md)** — Zero Trust et activation du mTLS ;
- **[docs/SECURITY-CHECKLIST.md](docs/SECURITY-CHECKLIST.md)** — check-list (Annexe A).

---

## 🧩 Stack technique

Spring Boot 3.3 · Spring Cloud Gateway 2023.0 · Spring Security (Resource Server OAuth2) ·
Keycloak 25 · HashiCorp Vault 1.17 · PostgreSQL 16 · Redis 7 · Micrometer/Prometheus · Grafana 11.

## ⚠️ Avertissement

Toutes les valeurs (mots de passe, tokens Vault, secrets clients, keystore auto-signé) sont
des **valeurs de démonstration**. Ne jamais les utiliser en production.