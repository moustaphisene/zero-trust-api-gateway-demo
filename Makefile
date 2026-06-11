# =====================================================================
# Démo Sécurisation des API REST — commandes utilitaires
# =====================================================================
.DEFAULT_GOAL := help
COMPOSE := docker compose

.PHONY: help up down logs ps build rebuild certs up-https token-user token-admin smoke clean

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

up: ## Démarre toute la stack (build + run)
	$(COMPOSE) up -d --build
	@echo "→ Keycloak : http://localhost:8180  (admin/admin)"
	@echo "→ Gateway  : http://localhost:8080"
	@echo "→ Grafana  : http://localhost:3000  (admin/admin)"
	@echo "→ Prometheus : http://localhost:9090"

down: ## Arrête la stack (conserve les volumes)
	$(COMPOSE) down

clean: ## Arrête et supprime volumes + images construites
	$(COMPOSE) down -v --rmi local

logs: ## Suit les logs de tous les services
	$(COMPOSE) logs -f

ps: ## État des conteneurs
	$(COMPOSE) ps

build: ## (Re)build les images applicatives
	$(COMPOSE) build

rebuild: ## Rebuild sans cache
	$(COMPOSE) build --no-cache

certs: ## Génère le keystore TLS auto-signé (profil https)
	bash infra/certs/generate-certs.sh

up-https: certs ## Démarre le Gateway en HTTPS (8443) — nécessite le keystore
	SPRING_PROFILES_ACTIVE=https $(COMPOSE) up -d --build
	@echo "→ Gateway HTTPS : https://localhost:8443"

mtls-certs: ## Génère la PKI mTLS (CA + certs serveur/client) dans infra/certs/
	bash infra/certs/generate-mtls-certs.sh

up-mtls: mtls-certs ## Démarre la stack avec mTLS Gateway ↔ microservices
	$(COMPOSE) -f docker-compose.yml -f docker-compose.mtls.yml up -d --build
	@echo "→ mTLS actif : Gateway appelle tender/user en https avec cert client"
	@echo "→ Preuve : curl -k https://localhost:8081/api/tenders  ⇒ rejet TLS (cert client requis)"

token-user: ## Récupère un access_token pour l'utilisateur 'user'
	@curl -s -X POST http://localhost:8180/realms/api-realm/protocol/openid-connect/token \
		-d grant_type=password -d client_id=api-gateway -d client_secret=gateway-secret-demo \
		-d username=user -d password=user123 | sed 's/,/,\n/g'

token-admin: ## Récupère un access_token pour 'admin'
	@curl -s -X POST http://localhost:8180/realms/api-realm/protocol/openid-connect/token \
		-d grant_type=password -d client_id=api-gateway -d client_secret=gateway-secret-demo \
		-d username=admin -d password=admin123 | sed 's/,/,\n/g'

smoke: ## Test de fumée : token user + appel /api/tenders via le Gateway
	@bash scripts/smoke-test.sh