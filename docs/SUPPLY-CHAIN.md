# 🔗 Sécurité de la chaîne d'approvisionnement logicielle

La sécurité ne s'arrête pas au code : il faut aussi garantir **ce qui entre** (dépendances)
et **ce qui sort** (images). Ce volet complète le pipeline DevSecOps (SAST, SCA, secret-scan,
image-scan) par la **traçabilité des composants** et l'**intégrité des artefacts**.

## Ce qui est en place (CI `.github/workflows/ci-secure.yml`)

| Maillon | Outil | Objectif |
|---------|-------|----------|
| SAST (code) | CodeQL | Vulnérabilités dans le code source |
| SCA (dépendances) | Trivy (fs) | CVE des bibliothèques Maven |
| Détection de secrets | Gitleaks | Aucun secret dans l'historique git |
| Scan d'images | Trivy (image) | CVE dans les couches des images |
| **SBOM** | CycloneDX (Maven) + Syft (image) | Inventaire signable des composants |
| **Signature & provenance** | cosign keyless (Sigstore) | Intégrité + origine vérifiables des images |

## SBOM (Software Bill of Materials)

Le plugin **CycloneDX** est lié à la phase `package` : chaque build produit
`*/target/classes/META-INF/sbom/application.cdx.json` — le SBOM est **embarqué dans le jar**,
inventaire complet des dépendances avec versions et licences.

```bash
mvn -DskipTests package          # génère les SBOM
cat tender-service/target/classes/META-INF/sbom/application.cdx.json | jq '.components | length'
```

Côté image, **Syft** (via `anchore/sbom-action`) génère un SBOM CycloneDX par image publiée.
Intérêt : réponse rapide à une CVE (« suis-je impacté ? ») et conformité (**OWASP API9/API10**,
ISO 27001 **A.5.21 / A.8.8**).

## Signature & attestation (cosign keyless)

Sur `push`, chaque image est poussée vers **GHCR**, **signée** avec `cosign` en mode
*keyless* (OIDC GitHub + transparence Sigstore/Rekor, sans clé à gérer), puis le **SBOM est
attesté** et rattaché à l'image par son *digest*.

```bash
# Côté consommateur : vérifier signature et provenance avant déploiement
cosign verify ghcr.io/<owner>/tender-service@sha256:... \
  --certificate-identity-regexp '.*' --certificate-oidc-issuer https://token.actions.githubusercontent.com
cosign verify-attestation --type cyclonedx ghcr.io/<owner>/tender-service@sha256:...
```

> Le job `supply-chain` ne s'exécute que sur `push` (les PR ne publient pas d'image) et
> requiert les permissions `packages: write` (GHCR) et `id-token: write` (OIDC cosign).

## Pourquoi c'est un sujet d'architecte

C'est la réponse concrète aux attaques de type **SolarWinds / dépendance compromise** :
- on **sait** ce qu'on déploie (SBOM),
- on **prouve** que l'image n'a pas été altérée et vient bien de notre CI (signature + provenance),
- on relie le tout à un cadre (ISO 27001 A.5.21 *Sécurité de la chaîne d'approvisionnement TIC*,
  SLSA, OWASP API Top 10). Voir `docs/COMPLIANCE.md`.

## Pour aller plus loin (production)

- Politique d'admission Kubernetes (Kyverno / Sigstore policy-controller) refusant toute image
  non signée.
- Niveau **SLSA** ≥ 3 (build isolé, provenance non falsifiable).
- Scan continu des images déployées (pas seulement au build).