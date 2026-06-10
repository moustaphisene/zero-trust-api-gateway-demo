#!/bin/sh
# =====================================================================
# Bootstrap Vault — provisionnement des secrets
# Provisionne les secrets servis au tender-service (clé AES, secret JWT).
# Exécuté en mode dev (token racine). En production : AppRole + TLS + DB engine.
# =====================================================================
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-root}"

echo "[vault-init] Attente de la disponibilité de Vault sur ${VAULT_ADDR} ..."
until vault status >/dev/null 2>&1; do
  sleep 2
done
echo "[vault-init] Vault est prêt."

# En mode dev, le secrets engine KV v2 est déjà monté sur 'secret/'.
# Clé maîtresse AES-256 (32 octets -> Base64) + secret JWT applicatif.
# (L'image Vault n'a pas openssl : on utilise /dev/urandom + base64 de busybox.)
MASTER_KEY="$(head -c 32 /dev/urandom | base64 | tr -d '\n')"
JWT_SECRET="$(head -c 64 /dev/urandom | base64 | tr -d '\n')"

echo "[vault-init] Écriture des secrets statiques secret/tender-service ..."
vault kv put secret/tender-service \
  app.encryption.master-key="${MASTER_KEY}" \
  jwt.secret="${JWT_SECRET}"

# Politique de moindre privilège : lecture seule sur le secret du service (Zero Trust).
echo "[vault-init] Application de la politique tender-service ..."
cat <<EOF | vault policy write tender-service -
path "secret/data/tender-service" {
  capabilities = ["read"]
}
EOF

echo "[vault-init] ✅ Secrets provisionnés."
echo "[vault-init] Vérification :"
vault kv get secret/tender-service