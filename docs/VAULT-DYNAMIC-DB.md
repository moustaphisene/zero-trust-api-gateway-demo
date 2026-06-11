# 🔑 Credentials PostgreSQL dynamiques (Vault Database engine)

Au lieu d'un mot de passe statique en clair, le **tender-service** obtient ses identifiants
PostgreSQL **à la volée** auprès de Vault. Vault crée un compte éphémère par bail (TTL court)
et le révoque automatiquement à l'expiration.

```bash
make up-vaultdb        # stack avec credentials DB dynamiques
make vaultdb-whoami    # affiche le compte v-token-… utilisé par le service
```

## Architecture

```
tender-service ──(spring.cloud.vault.database)──► Vault ──(crée le compte)──► PostgreSQL
       │                                            │
       └───── se connecte avec « v-token-… » ───────┘  (compte éphémère, TTL 1h)
```

| Pièce | Fichier |
|-------|---------|
| Configuration du moteur Database (connexion + rôle dynamique) | `infra/vault/configure-db-engine.sh` |
| Service d'init Vault (one-shot, après Vault + Postgres) | `docker-compose.vaultdb.yml` → `vault-db-init` |
| Activation côté service (profil + variables d'env) | `tender-service/.../application-vaultdb.yml` + `docker-compose.vaultdb.yml` |

## Le piège de l'ownership (et sa solution)

Les comptes dynamiques sont **révoqués** (`DROP ROLE`) à l'expiration du bail. Or un rôle ne
peut être supprimé s'il **possède** des objets (tables, séquences). Si Hibernate créait les
tables sous le compte éphémère, la révocation échouerait.

**Solution** — `creation_statements` du rôle Vault :

```sql
CREATE ROLE "{{name}}" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'
  IN ROLE tender_user;
ALTER ROLE "{{name}}" SET ROLE tender_user;
```

- `IN ROLE tender_user` : le compte éphémère est membre du rôle stable `tender_user`.
- `SET ROLE tender_user` : à chaque connexion, le compte bascule automatiquement sur
  `tender_user`. Tout objet créé (tables Hibernate, lignes insérées) appartient donc à
  `tender_user`. Le compte éphémère **ne possède rien** ⇒ révocation propre.

## Subtilité d'activation (config.import)

Le backend `database` doit être configuré **avant** la phase `spring.config.import` qui
contacte Vault. Les propriétés `spring.cloud.vault.database.*` sont donc passées en
**variables d'environnement** (`SPRING_CLOUD_VAULT_DATABASE_*`) dans
`docker-compose.vaultdb.yml` — disponibles dès le démarrage, contrairement à un fichier de
profil qui serait lu trop tard.

Dépendance requise : `org.springframework.cloud:spring-cloud-vault-config-databases`
(le starter core ne contient pas le backend Database).

## Vérifications

```bash
# Le service se connecte avec un compte éphémère (pas tender_user)
docker compose exec postgres psql -U postgres -d tender_db \
  -c "select distinct usename from pg_stat_activity where datname='tender_db';"
#  → v-token-tender-s-XXXXXXXX-...

# Vault génère un credential frais à la demande
docker compose exec vault sh -c \
  'VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault read database/creds/tender-service'
#  → username=v-token-..., lease_duration=3600, renewable=true
```

## Production

- TTL plus courts + renouvellement de bail (déjà activé via `spring.cloud.vault.config.lifecycle`).
- Rotation **hot** du pool Hikari sans redémarrage (les nouvelles connexions prennent les
  credentials renouvelés ; `max-lifetime` recycle les anciennes).
- Compte de connexion Vault dédié (least-privilege) plutôt que le superuser `postgres`.
- Auth Vault par AppRole/Kubernetes plutôt que token racine.