#!/usr/bin/env bash
# =====================================================================
# Génération d'un keystore PKCS12 auto-signé pour HTTPS/TLS du Gateway
# Réf. 1.3.3 (Listing 1.3). DÉVELOPPEMENT UNIQUEMENT.
# En production : certificat émis par une CA reconnue (Let's Encrypt, DigiCert...).
# =====================================================================
set -e
CERT_DIR="$(cd "$(dirname "$0")" && pwd)"
KEYSTORE="${CERT_DIR}/keystore.p12"
PASSWORD="${SSL_KEYSTORE_PASSWORD:-changeit}"

if [ -f "${KEYSTORE}" ]; then
  echo "[certs] ${KEYSTORE} existe déjà — rien à faire."
  exit 0
fi

echo "[certs] Génération du keystore auto-signé ..."
keytool -genkeypair \
  -alias api-gateway \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore "${KEYSTORE}" \
  -validity 365 \
  -storepass "${PASSWORD}" \
  -dname "CN=localhost, OU=Dev, O=SEN IT SERVICES PRO, L=Dakar, C=SN" \
  -ext "SAN=dns:localhost,dns:gateway,ip:127.0.0.1"

echo "[certs] ✅ Keystore créé : ${KEYSTORE} (alias=api-gateway, mdp=${PASSWORD})"