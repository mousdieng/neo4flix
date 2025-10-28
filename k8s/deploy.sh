#!/bin/bash

# Neo4flix Kubernetes Deployment Script
# This script deploys the Neo4flix application to a local Kubernetes cluster

set -e

echo "====================================="
echo "Neo4flix Kubernetes Deployment"
echo "====================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl not found. Please install kubectl first."
    exit 1
fi

# Check if cluster is accessible
if ! kubectl cluster-info &> /dev/null; then
    print_error "Cannot connect to Kubernetes cluster. Please ensure your cluster is running."
    print_info "For local development, you can use:"
    print_info "  - Minikube: minikube start"
    print_info "  - Kind: kind create cluster"
    print_info "  - Docker Desktop: Enable Kubernetes in settings"
    exit 1
fi

print_info "Kubernetes cluster is accessible"
echo ""

# Step 1: Create namespace
print_info "Step 1: Creating namespace..."
kubectl apply -f config/namespace.yaml
echo ""

# Step 2: Create ConfigMap
print_info "Step 2: Creating ConfigMap..."
kubectl apply -f config/configmap.yaml
echo ""

# Step 3: Create PersistentVolumeClaims
print_info "Step 3: Creating PersistentVolumeClaims..."
kubectl apply -f infrastructure/persistent-volumes.yaml
echo ""

# Wait for PVCs to be bound
print_info "Waiting for PVCs to be bound..."
kubectl wait --for=jsonpath='{.status.phase}'=Bound pvc/neo4j-data-pvc -n neo4flix --timeout=60s || true
echo ""

# Step 4: Deploy infrastructure services
print_info "Step 4: Deploying infrastructure services..."
print_info "  - Deploying Zookeeper..."
kubectl apply -f infrastructure/zookeeper.yaml
sleep 5

print_info "  - Deploying Kafka..."
kubectl apply -f infrastructure/kafka.yaml
sleep 5

print_info "  - Deploying Redis..."
kubectl apply -f infrastructure/redis.yaml
sleep 5

print_info "  - Deploying Neo4j..."
kubectl apply -f infrastructure/neo4j.yaml
sleep 5

print_info "  - Deploying Kafka UI..."
kubectl apply -f infrastructure/kafka-ui.yaml
echo ""

# Wait for infrastructure to be ready
print_info "Waiting for infrastructure services to be ready..."
kubectl wait --for=condition=available --timeout=180s deployment/zookeeper -n neo4flix || true
kubectl wait --for=condition=available --timeout=180s deployment/kafka -n neo4flix || true
kubectl wait --for=condition=available --timeout=180s deployment/redis -n neo4flix || true
kubectl wait --for=condition=available --timeout=180s deployment/neo4j -n neo4flix || true
echo ""

# Step 5: Deploy microservices
print_info "Step 5: Deploying microservices..."
print_info "  - Deploying User Service..."
kubectl apply -f microservices/user-service.yaml

print_info "  - Deploying Movie Service..."
kubectl apply -f microservices/movie-service.yaml

print_info "  - Deploying Rating Service..."
kubectl apply -f microservices/rating-service.yaml

print_info "  - Deploying Recommendation Service..."
kubectl apply -f microservices/recommendation-service.yaml

print_info "  - Deploying Watchlist Service..."
kubectl apply -f microservices/watchlist-service.yaml

print_info "  - Deploying Gateway Service..."
kubectl apply -f microservices/gateway-service.yaml
echo ""

# Wait for microservices to be ready
print_info "Waiting for microservices to be ready (this may take a few minutes)..."
kubectl wait --for=condition=available --timeout=300s deployment/user-service -n neo4flix || true
kubectl wait --for=condition=available --timeout=300s deployment/movie-service -n neo4flix || true
kubectl wait --for=condition=available --timeout=300s deployment/rating-service -n neo4flix || true
kubectl wait --for=condition=available --timeout=300s deployment/recommendation-service -n neo4flix || true
kubectl wait --for=condition=available --timeout=300s deployment/watchlist-service -n neo4flix || true
kubectl wait --for=condition=available --timeout=300s deployment/gateway-service -n neo4flix || true
echo ""

# Display deployment status
print_info "Deployment Status:"
echo ""
kubectl get all -n neo4flix
echo ""

# Display access information
print_info "====================================="
print_info "Deployment Complete!"
print_info "====================================="
echo ""
print_info "Access the services at:"
echo ""
print_info "Gateway Service (API):      http://localhost:30080"
print_info "Neo4j Browser:              http://localhost:30474 (username: neo4j, password: password)"
print_info "Kafka UI:                   http://localhost:30091"
echo ""
print_info "To view logs:"
print_info "  kubectl logs -f deployment/<service-name> -n neo4flix"
echo ""
print_info "To scale a service:"
print_info "  kubectl scale deployment/<service-name> --replicas=<number> -n neo4flix"
echo ""
print_info "To delete all resources:"
print_info "  kubectl delete namespace neo4flix"
echo ""
