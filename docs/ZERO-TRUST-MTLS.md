# 🛡️ Zero Trust & mTLS (réf. 9.5)

## Principe

> « Ne jamais faire confiance, toujours vérifier. »

Contrairement au modèle périmétrique (château fort), chaque requête est traitée comme
potentiellement malveillante, qu'elle vienne de l'extérieur ou de l'intérieur du réseau.

## Ce qui est implémenté dans la démo

| Principe Zero Trust (Table 9.2) | Implémentation |
|---------------------------------|----------------|
| **Vérification continue** | Le Gateway valide le JWT **et** chaque microservice le **re-valide** (signature RS256 + expiration). Un accès direct à `tender-service:8081` sans token renvoie `401`. |
| **Moindre privilège** | RBAC (rôles composites Keycloak) + permissions fines (`tender:read/write/delete`, `user:manage`) + ABAC contextuel. |
| **Micro-segmentation** | Réseau Docker dédié `secnet` ; en Kubernetes : NetworkPolicies (voir ci-dessous). |
| **Chiffrement bout-en-bout** | TLS au edge (profil `https`) ; mTLS entre services (procédure ci-dessous). |
| **Monitoring continu** | Journalisation structurée `SECURITY_AUDIT` + métriques Prometheus + alertes. |

### Anti-usurpation d'en-têtes

Le filtre `JwtValidationGatewayFilterFactory` **supprime** tout en-tête `X-User-Id`,
`X-User-Roles`, `X-Tenant-Id`, `X-Department` fourni par le client, puis les **reconstruit
exclusivement** à partir des claims du token validé. Un client qui injecte
`X-User-Roles: ADMIN` ne gagne aucun privilège (requête Postman `2 → Header spoofing`).

## Activer le mTLS entre Gateway et microservices

> Non activé par défaut pour garder la démo « 1 commande ». Procédure de durcissement :

1. **Générer une PKI** (CA + certs serveur/client) :
   ```bash
   # CA
   openssl req -x509 -newkey rsa:2048 -nodes -keyout ca.key -out ca.crt -days 365 -subj "/CN=demo-ca"
   # Cert service (répéter pour gateway, tender-service, user-service)
   openssl req -newkey rsa:2048 -nodes -keyout svc.key -out svc.csr -subj "/CN=tender-service"
   openssl x509 -req -in svc.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out svc.crt -days 365
   ```
2. **Côté microservice** (`application.yml`) : exiger le certificat client.
   ```yaml
   server:
     ssl:
       enabled: true
       client-auth: need          # mTLS : certificat client obligatoire
       trust-store: file:/certs/truststore.p12
       trust-store-password: ${TRUSTSTORE_PASS}
   ```
3. **Côté Gateway** : présenter le certificat client via un `WebClient`/HttpClient configuré
   avec le keystore client et le truststore de la CA.
4. **Kubernetes (recommandé en production)** : déléguer le mTLS à un *service mesh*
   (Istio, Linkerd) en mode `STRICT`, plutôt que de le gérer dans le code.

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