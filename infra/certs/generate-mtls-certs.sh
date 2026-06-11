#!/usr/bin/env bash
# =====================================================================
# Génération de la PKI mTLS (DÉVELOPPEMENT UNIQUEMENT)
#   - une autorité de certification (CA) racine
#   - un certificat SERVEUR par microservice (SAN = nom de service docker)
#   - un certificat CLIENT pour le Gateway
#   - un truststore PKCS12 contenant la CA (validation des pairs)
#
# Tous les keystores sont protégés par le mot de passe $MTLS_PASSWORD (défaut: changeit).
# En production : CA d'entreprise ou service mesh (Istio/Linkerd) en mode mTLS STRICT.
# =====================================================================
set -euo pipefail
CERT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${CERT_DIR}"
PASSWORD="${MTLS_PASSWORD:-changeit}"
DAYS=825

if [ -f truststore.p12 ] && [ -f gateway-client.p12 ]; then
  echo "[mtls] PKICertificats déjà présents dans ${CERT_DIR} — rien à faire (supprimez-les pour régénérer)."
  exit 0
fi

echo "[mtls] 1) Autorité de certification (CA)…"
openssl req -x509 -newkey rsa:2048 -nodes -keyout ca.key -out ca.crt -days "${DAYS}" \
  -subj "/CN=demo-mtls-ca/O=SEN IT SERVICES PRO/C=SN"

gen_cert() {  # $1=nom  $2=eku (serverAuth|clientAuth)  $3=SAN-extra
  local name="$1" eku="$2" san="$3"
  echo "[mtls]    - certificat ${name} (${eku})…"
  openssl req -newkey rsa:2048 -nodes -keyout "${name}.key" -out "${name}.csr" -subj "/CN=${name}"
  openssl x509 -req -in "${name}.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out "${name}.crt" -days "${DAYS}" \
    -extfile <(printf "basicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=%s\n%s" "${eku}" "${san}")
  # Keystore PKCS12 (clé + cert + chaîne CA)
  openssl pkcs12 -export -in "${name}.crt" -inkey "${name}.key" -certfile ca.crt \
    -name "${name}" -out "${name}.p12" -passout "pass:${PASSWORD}"
}

echo "[mtls] 2) Certificats SERVEUR (tender-service, user-service)…"
gen_cert tender-service serverAuth "subjectAltName=DNS:tender-service,DNS:localhost,IP:127.0.0.1"
gen_cert user-service   serverAuth "subjectAltName=DNS:user-service,DNS:localhost,IP:127.0.0.1"

echo "[mtls] 3) Certificat CLIENT (Gateway)…"
gen_cert gateway-client clientAuth "subjectAltName=DNS:api-gateway"

echo "[mtls] 4) Truststore PKCS12 (CA de confiance partagée)…"
keytool -importcert -noprompt -trustcacerts -alias demo-mtls-ca \
  -file ca.crt -keystore truststore.p12 -storetype PKCS12 -storepass "${PASSWORD}"

# Nettoyage des fichiers intermédiaires (on garde ca.crt pour le Gateway).
rm -f ./*.csr ca.srl

echo "[mtls] ✅ PKI générée dans ${CERT_DIR} :"
echo "        ca.crt, truststore.p12, tender-service.p12, user-service.p12, gateway-client.p12"
echo "        (mot de passe des keystores : ${PASSWORD})"