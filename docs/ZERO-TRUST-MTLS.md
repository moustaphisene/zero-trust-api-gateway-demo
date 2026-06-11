# 🛡️ Zero Trust & mTLS

## Principe

> « Ne jamais faire confiance, toujours vérifier. »

Contrairement au modèle périmétrique (château fort), chaque requête est traitée comme
potentiellement malveillante, qu'elle vienne de l'extérieur ou de l'intérieur du réseau.

## Ce qui est implémenté dans la démo

| Principe Zero Trust | Implémentation |
|---------------------------------|----------------|
| **Vérification continue** | Le Gateway valide le JWT **et** chaque microservice le **re-valide** (signature RS256 + expiration). Un accès direct à `tender-service:8081` sans token renvoie `401`. |
| **Moindre privilège** | RBAC (rôles composites Keycloak) + permissions fines (`tender:read/write/delete`, `user:manage`) + ABAC contextuel. |
| **Micro-segmentation** | Réseau Docker dédié `secnet` ; en Kubernetes : NetworkPolicies (voir ci-dessous). |
| **Chiffrement bout-en-bout** | TLS au edge (profil `https`) ; **mTLS entre services** (profil `mtls`, `make up-mtls`). |
| **Monitoring continu** | Journalisation structurée `SECURITY_AUDIT` + métriques Prometheus + alertes. |

### Anti-usurpation d'en-têtes

Le filtre `JwtValidationGatewayFilterFactory` **supprime** tout en-tête `X-User-Id`,
`X-User-Roles`, `X-Tenant-Id`, `X-Department` fourni par le client, puis les **reconstruit
exclusivement** à partir des claims du token validé. Un client qui injecte
`X-User-Roles: ADMIN` ne gagne aucun privilège (requête Postman `2 → Header spoofing`).

## mTLS entre Gateway et microservices (implémenté)

Le mTLS est **réellement câblé** via le profil Spring `mtls`. Il n'est pas activé par défaut
pour garder le démarrage « 1 commande », mais s'active en une seule commande :

```bash
make up-mtls    # génère la PKI puis démarre la stack avec mTLS
```

### Pièces du puzzle

| Élément | Fichier |
|---------|---------|
| Génération PKI (CA, certs serveur/client, truststore) | `infra/certs/generate-mtls-certs.sh` |
| Serveur mTLS tender-service (`client-auth: need`) | `tender-service/src/main/resources/application-mtls.yml` |
| Serveur mTLS user-service | `user-service/src/main/resources/application-mtls.yml` |
| Client mTLS du Gateway (`httpclient.ssl`) | `gateway/src/main/resources/application-mtls.yml` |
| Orchestration (profils, montage `/certs`, URIs https) | `docker-compose.mtls.yml` |

### Fonctionnement

1. **PKI** : une CA racine signe un certificat **serveur** par microservice (SAN = nom de
   service docker) et un certificat **client** pour le Gateway. Un truststore PKCS12 contient
   la CA et sert à valider les pairs.
2. **Microservices** : `server.ssl.client-auth=need` ⇒ le service n'accepte QUE des clients
   présentant un certificat signé par la CA. L'écoute passe en HTTPS (8081 / 8082).
3. **Gateway** : `spring.cloud.gateway.httpclient.ssl` présente le certificat client et ne fait
   confiance qu'aux serveurs signés par la CA (`trusted-x509-certificates: /certs/ca.crt`) ;
   les routes pointent vers `https://tender-service:8081` et `https://user-service:8082`.

### Preuve d'application

Un appel direct à un microservice **sans certificat client** est rejeté dès la poignée de
main TLS (avant même la couche applicative) :

```bash
curl -k https://localhost:8081/api/tenders     # ⇒ échec handshake : certificat client requis
```

### Production

En production, déléguer de préférence le mTLS à un *service mesh* (Istio, Linkerd) en mode
`STRICT`, et faire émettre les certificats par une CA d'entreprise (ou SPIFFE/SPIRE) avec
rotation automatique — plutôt que de gérer les keystores manuellement.

## Micro-segmentation Kubernetes (exemple)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata: { name: tender-allow-gateway-only }
spec:
  podSelector: { matchLabels: { app: tender-service } }
  policyTypes: [ Ingress ]
  ingress:
    - from:
        - podSelector: { matchLabels: { app: api-gateway } }
      ports:
        - { protocol: TCP, port: 8081 }
```