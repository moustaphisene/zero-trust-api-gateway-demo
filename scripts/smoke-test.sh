#!/usr/bin/env bash
# =====================================================================
# Test de fumée — vérifie la chaîne complète : Keycloak → token → Gateway → service
# =====================================================================
set -euo pipefail

KEYCLOAK="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY="${GATEWAY_URL:-http://localhost:8080}"

echo " 1) Obtention d'un token (utilisateur 'user')…"
TOKEN=$(curl -s -X POST "${KEYCLOAK}/realms/api-realm/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=api-gateway -d client_secret=gateway-secret-demo \
  -d username=user -d password=user123 | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "${TOKEN}" ]; then
  echo "✗ Échec : aucun token. Keycloak est-il prêt ? (docker compose ps)"
  exit 1
fi
echo "✓ Token obtenu (${#TOKEN} caractères)."

echo " 2) Appel SANS token (attendu 401)…"
code=$(curl -s -o /dev/null -w '%{http_code}' "${GATEWAY}/api/tenders")
[ "${code}" = "401" ] && echo "✓ 401 reçu." || { echo "✗ Attendu 401, reçu ${code}"; exit 1; }

echo " 3) Appel AVEC token (attendu 200)…"
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${TOKEN}" "${GATEWAY}/api/tenders")
[ "${code}" = "200" ] && echo "✓ 200 reçu." || { echo "✗ Attendu 200, reçu ${code}"; exit 1; }

echo " 4) Création par 'user' (attendu 403 — RBAC)…"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"title":"x","amount":1,"department":"FINANCE"}' "${GATEWAY}/api/tenders")
[ "${code}" = "403" ] && echo "✓ 403 reçu." || { echo "✗ Attendu 403, reçu ${code}"; exit 1; }

echo ""
echo " Test de fumée réussi — la chaîne JWT / Gateway / RBAC fonctionne."