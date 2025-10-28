#!/bin/bash

# Neo4flix Docker Image Build Script
# This script builds all microservice Docker images for Kubernetes deployment

set -e

echo "====================================="
echo "Neo4flix Docker Images Builder"
echo "====================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Navigate to project root
cd ..

print_info "Building microservices Docker images..."
echo ""

# Build User Service
print_info "Building User Service..."
cd microservices/user-service
mvn clean package -DskipTests
docker build -t neo4flix/user-service:latest .
cd ../..
echo ""

# Build Movie Service
print_info "Building Movie Service..."
cd microservices/movie-service
mvn clean package -DskipTests
docker build -t neo4flix/movie-service:latest .
cd ../..
echo ""

# Build Rating Service
print_info "Building Rating Service..."
cd microservices/rating-service
mvn clean package -DskipTests
docker build -t neo4flix/rating-service:latest .
cd ../..
echo ""

# Build Recommendation Service
print_info "Building Recommendation Service..."
cd microservices/recommendation-service
mvn clean package -DskipTests
docker build -t neo4flix/recommendation-service:latest .
cd ../..
echo ""

# Build Watchlist Service
print_info "Building Watchlist Service..."
cd microservices/watchlist-service
mvn clean package -DskipTests
docker build -t neo4flix/watchlist-service:latest .
cd ../..
echo ""

# Build Gateway Service
print_info "Building Gateway Service..."
cd microservices/gateway-service
mvn clean package -DskipTests
docker build -t neo4flix/gateway-service:latest .
cd ../..
echo ""

print_info "====================================="
print_info "Build Complete!"
print_info "====================================="
echo ""
print_info "All microservice images have been built successfully."
print_info "You can now deploy to Kubernetes using: ./k8s/deploy.sh"
echo ""
print_info "Available images:"
docker images | grep neo4flix
echo ""
