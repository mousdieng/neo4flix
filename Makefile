.PHONY: help install build run stop clean logs status restart run-infra run-services run-frontend run-all

# Colors for output
CYAN := \033[0;36m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

# Service directories
REGISTRY_DIR := microservices/registry-service
GATEWAY_DIR := microservices/gateway-service
MOVIE_DIR := microservices/movie-service
USER_DIR := microservices/user-service
RATING_DIR := microservices/rating-service
RECOMMENDATION_DIR := microservices/recommendation-service
FRONTEND_DIR := frontend

# Maven command
MVN := mvn

# Ports
REGISTRY_PORT := 8761
GATEWAY_PORT := 8080
MOVIE_PORT := 8081
USER_PORT := 8082
RATING_PORT := 8083
RECOMMENDATION_PORT := 8084
FRONTEND_PORT := 4200

help: ## Show this help message
	@echo "$(CYAN)Neo4flix Microservices - Available Commands$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'

install: ## Install dependencies for all services
	@echo "$(CYAN)Installing dependencies...$(NC)"
	@echo "$(YELLOW)Installing Maven dependencies for microservices...$(NC)"
	@cd $(REGISTRY_DIR) && $(MVN) clean install -DskipTests
	@cd $(GATEWAY_DIR) && $(MVN) clean install -DskipTests
	@cd $(MOVIE_DIR) && $(MVN) clean install -DskipTests
	@cd $(USER_DIR) && $(MVN) clean install -DskipTests
	@cd $(RATING_DIR) && $(MVN) clean install -DskipTests
	@cd $(RECOMMENDATION_DIR) && $(MVN) clean install -DskipTests
	@echo "$(YELLOW)Installing npm dependencies for frontend...$(NC)"
	@cd $(FRONTEND_DIR) && npm install
	@echo "$(GREEN)All dependencies installed successfully!$(NC)"

build: ## Build all services
	@echo "$(CYAN)Building all services...$(NC)"
	@cd $(REGISTRY_DIR) && $(MVN) package -DskipTests &
	@cd $(GATEWAY_DIR) && $(MVN) package -DskipTests &
	@cd $(MOVIE_DIR) && $(MVN) package -DskipTests &
	@cd $(USER_DIR) && $(MVN) package -DskipTests &
	@cd $(RATING_DIR) && $(MVN) package -DskipTests &
	@cd $(RECOMMENDATION_DIR) && $(MVN) package -DskipTests &
	@wait
	@echo "$(GREEN)All services built successfully!$(NC)"

run-infra: ## Start infrastructure (Neo4j, Redis)
	@echo "$(CYAN)Starting infrastructure services...$(NC)"
	@docker-compose up -d neo4j redis
	@echo "$(GREEN)Infrastructure started!$(NC)"
	@echo "$(YELLOW)Neo4j: http://localhost:7474 (neo4j/password)$(NC)"
	@echo "$(YELLOW)Redis: localhost:6379$(NC)"

run-registry: ## Run registry service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Registry Service on port $(REGISTRY_PORT)...$(NC)"
	@cd $(REGISTRY_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/registry.log 2>&1 & echo $$! > ../../logs/registry.pid)
	@echo "$(GREEN)Registry Service started! PID: $$(cat logs/registry.pid)$(NC)"

run-gateway: ## Run gateway service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Gateway Service on port $(GATEWAY_PORT)...$(NC)"
	@cd $(GATEWAY_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/gateway.log 2>&1 & echo $$! > ../../logs/gateway.pid)
	@echo "$(GREEN)Gateway Service started! PID: $$(cat logs/gateway.pid)$(NC)"

run-movie: ## Run movie service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Movie Service on port $(MOVIE_PORT)...$(NC)"
	@cd $(MOVIE_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/movie.log 2>&1 & echo $$! > ../../logs/movie.pid)
	@echo "$(GREEN)Movie Service started! PID: $$(cat logs/movie.pid)$(NC)"

run-user: ## Run user service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting User Service on port $(USER_PORT)...$(NC)"
	@cd $(USER_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/user.log 2>&1 & echo $$! > ../../logs/user.pid)
	@echo "$(GREEN)User Service started! PID: $$(cat logs/user.pid)$(NC)"

run-rating: ## Run rating service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Rating Service on port $(RATING_PORT)...$(NC)"
	@cd $(RATING_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/rating.log 2>&1 & echo $$! > ../../logs/rating.pid)
	@echo "$(GREEN)Rating Service started! PID: $$(cat logs/rating.pid)$(NC)"

run-recommendation: ## Run recommendation service in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Recommendation Service on port $(RECOMMENDATION_PORT)...$(NC)"
	@cd $(RECOMMENDATION_DIR) && (mkdir -p ../../logs; nohup $(MVN) spring-boot:run > ../../logs/recommendation.log 2>&1 & echo $$! > ../../logs/recommendation.pid)
	@echo "$(GREEN)Recommendation Service started! PID: $$(cat logs/recommendation.pid)$(NC)"

run-frontend: ## Run frontend in background
	@mkdir -p logs
	@echo "$(CYAN)Starting Frontend on port $(FRONTEND_PORT)...$(NC)"
	@cd $(FRONTEND_DIR) && (mkdir -p ../logs; nohup npm start > ../logs/frontend.log 2>&1 & echo $$! > ../logs/frontend.pid)
	@echo "$(GREEN)Frontend started! PID: $$(cat logs/frontend.pid)$(NC)"

run-services: ## Run all microservices in background (sequential startup)
	@mkdir -p logs
	@echo "$(CYAN)Starting all microservices...$(NC)"
	@$(MAKE) run-registry
	@echo "$(YELLOW)Waiting 30s for Registry Service to start...$(NC)"
	@sleep 30
	@$(MAKE) run-movie &
	@$(MAKE) run-user &
	@$(MAKE) run-rating &
	@$(MAKE) run-recommendation &
	@echo "$(YELLOW)Waiting 20s for services to register...$(NC)"
	@sleep 20
	@$(MAKE) run-gateway
	@echo "$(GREEN)All microservices started!$(NC)"

run-all: run-infra run-services run-frontend ## Start infrastructure, all microservices, and frontend

run: run-all ## Alias for run-all

stop-services: ## Stop only microservices and frontend (keep infrastructure running)
	@bash scripts/stop-services.sh

stop: ## Stop all services (microservices, frontend, and infrastructure)
	@echo "$(CYAN)Stopping all services...$(NC)"
	@if [ -f logs/registry.pid ]; then kill $$(cat logs/registry.pid) 2>/dev/null || true; rm logs/registry.pid; fi
	@if [ -f logs/gateway.pid ]; then kill $$(cat logs/gateway.pid) 2>/dev/null || true; rm logs/gateway.pid; fi
	@if [ -f logs/movie.pid ]; then kill $$(cat logs/movie.pid) 2>/dev/null || true; rm logs/movie.pid; fi
	@if [ -f logs/user.pid ]; then kill $$(cat logs/user.pid) 2>/dev/null || true; rm logs/user.pid; fi
	@if [ -f logs/rating.pid ]; then kill $$(cat logs/rating.pid) 2>/dev/null || true; rm logs/rating.pid; fi
	@if [ -f logs/recommendation.pid ]; then kill $$(cat logs/recommendation.pid) 2>/dev/null || true; rm logs/recommendation.pid; fi
	@if [ -f logs/frontend.pid ]; then kill $$(cat logs/frontend.pid) 2>/dev/null || true; rm logs/frontend.pid; fi
	@docker-compose down
	@echo "$(GREEN)All services stopped!$(NC)"

clean: stop ## Stop services and clean build artifacts
	@echo "$(CYAN)Cleaning build artifacts...$(NC)"
	@cd $(REGISTRY_DIR) && $(MVN) clean || true
	@cd $(GATEWAY_DIR) && $(MVN) clean || true
	@cd $(MOVIE_DIR) && $(MVN) clean || true
	@cd $(USER_DIR) && $(MVN) clean || true
	@cd $(RATING_DIR) && $(MVN) clean || true
	@cd $(RECOMMENDATION_DIR) && $(MVN) clean || true
	@rm -rf logs
	@echo "$(GREEN)Clean complete!$(NC)"

logs: ## Show logs from all services
	@echo "$(CYAN)Tailing all service logs...$(NC)"
	@tail -f logs/*.log 2>/dev/null || echo "$(RED)No log files found. Services may not be running.$(NC)"

logs-registry: ## Show registry service logs
	@tail -f logs/registry.log 2>/dev/null || echo "$(RED)Registry log not found$(NC)"

logs-gateway: ## Show gateway service logs
	@tail -f logs/gateway.log 2>/dev/null || echo "$(RED)Gateway log not found$(NC)"

logs-movie: ## Show movie service logs
	@tail -f logs/movie.log 2>/dev/null || echo "$(RED)Movie service log not found$(NC)"

logs-user: ## Show user service logs
	@tail -f logs/user.log 2>/dev/null || echo "$(RED)User service log not found$(NC)"

logs-rating: ## Show rating service logs
	@tail -f logs/rating.log 2>/dev/null || echo "$(RED)Rating service log not found$(NC)"

logs-recommendation: ## Show recommendation service logs
	@tail -f logs/recommendation.log 2>/dev/null || echo "$(RED)Recommendation service log not found$(NC)"

logs-frontend: ## Show frontend logs
	@tail -f logs/frontend.log 2>/dev/null || echo "$(RED)Frontend log not found$(NC)"

status: ## Check status of all services
	@echo "$(CYAN)Service Status:$(NC)"
	@echo ""
	@echo "$(YELLOW)Infrastructure:$(NC)"
	@docker ps --filter "name=neo4flix-neo4j" --format "  Neo4j: {{.Status}}" || echo "  Neo4j: $(RED)Not running$(NC)"
	@docker ps --filter "name=neo4flix-redis" --format "  Redis: {{.Status}}" || echo "  Redis: $(RED)Not running$(NC)"
	@echo ""
	@echo "$(YELLOW)Microservices:$(NC)"
	@if [ -f logs/registry.pid ] && kill -0 $$(cat logs/registry.pid) 2>/dev/null; then echo "  Registry: $(GREEN)Running (PID: $$(cat logs/registry.pid))$(NC)"; else echo "  Registry: $(RED)Not running$(NC)"; fi
	@if [ -f logs/gateway.pid ] && kill -0 $$(cat logs/gateway.pid) 2>/dev/null; then echo "  Gateway: $(GREEN)Running (PID: $$(cat logs/gateway.pid))$(NC)"; else echo "  Gateway: $(RED)Not running$(NC)"; fi
	@if [ -f logs/movie.pid ] && kill -0 $$(cat logs/movie.pid) 2>/dev/null; then echo "  Movie: $(GREEN)Running (PID: $$(cat logs/movie.pid))$(NC)"; else echo "  Movie: $(RED)Not running$(NC)"; fi
	@if [ -f logs/user.pid ] && kill -0 $$(cat logs/user.pid) 2>/dev/null; then echo "  User: $(GREEN)Running (PID: $$(cat logs/user.pid))$(NC)"; else echo "  User: $(RED)Not running$(NC)"; fi
	@if [ -f logs/rating.pid ] && kill -0 $$(cat logs/rating.pid) 2>/dev/null; then echo "  Rating: $(GREEN)Running (PID: $$(cat logs/rating.pid))$(NC)"; else echo "  Rating: $(RED)Not running$(NC)"; fi
	@if [ -f logs/recommendation.pid ] && kill -0 $$(cat logs/recommendation.pid) 2>/dev/null; then echo "  Recommendation: $(GREEN)Running (PID: $$(cat logs/recommendation.pid))$(NC)"; else echo "  Recommendation: $(RED)Not running$(NC)"; fi
	@if [ -f logs/frontend.pid ] && kill -0 $$(cat logs/frontend.pid) 2>/dev/null; then echo "  Frontend: $(GREEN)Running (PID: $$(cat logs/frontend.pid))$(NC)"; else echo "  Frontend: $(RED)Not running$(NC)"; fi

restart: stop run ## Restart all services

dev: ## Start infrastructure and run services in development mode
	@$(MAKE) run-infra
	@echo "$(YELLOW)Infrastructure started. You can now run services individually with 'make run-<service>'$(NC)"
