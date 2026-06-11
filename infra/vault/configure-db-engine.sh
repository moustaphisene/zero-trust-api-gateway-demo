#!/bin/sh
# =====================================================================
# Configuration du moteur Database de Vault — credentials PostgreSQL DYNAMIQUES.
# Vault crée, à la demande, des comptes PostgreSQL éphémères (TTL court) et les
# révoque automatiquement à l'expiration du bail.
#
# Astuce d'ownership : chaque compte dynamique est créé IN ROLE tender_user avec
# « ALTER ROLE … SET ROLE tender_user » ⇒ tout objet créé appartient au rôle stable
# tender_user. Le compte éphémère ne possède donc RIEN et peut être révoqué (DROP ROLE)
# proprement à l'expiration.
# =====================================================================
set -e
export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-root}"

echo "[db-engine] Attente de Vault…"
until vault status >/dev/null 2>&1; do sleep 2; done

echo "[db-engine] Activation du secrets engine 'database'…"
vault secrets enable -path=database database 2>/dev/null || echo "[db-engine] (déjà activé)"

echo "[db-engine] Configuration de la connexion PostgreSQL (Vault se connecte en superuser)…"
vault write database/config/tender-db \
  plugin_name=postgresql-database-plugin \
  allowed_roles="tender-service" \
  connection_url="postgresql://{{username}}:{{password}}@postgres:5432/tender_db?sslmode=disable" \
  username="postgres" \
  password="postgres"

echo "[db-engine] Création du rôle dynamique 'tender-service' (TTL 1h, max 24h)…"
vault write database/roles/tender-service \
  db_name=tender-db \
  creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE tender_user; ALTER ROLE \"{{name}}\" SET ROLE tender_user;" \
  default_ttl="1h" \
  max_ttl="24h"

echo "[db-engine] ✅ Moteur Database configuré."
echo "[db-engine] Démonstration — credential dynamique généré à la volée :"
vault read database/creds/tender-service