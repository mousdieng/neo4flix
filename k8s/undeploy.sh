#!/bin/bash

# Neo4flix Kubernetes Cleanup Script
# This script removes all Neo4flix resources from Kubernetes

set -e

echo "====================================="
echo "Neo4flix Kubernetes Cleanup"
echo "====================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Confirm deletion
read -p "Are you sure you want to delete all Neo4flix resources? This will delete all data! (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    print_info "Cleanup cancelled."
    exit 0
fi

echo ""
print_warning "Deleting all Neo4flix resources..."
echo ""

# Delete microservices
print_info "Deleting microservices..."
kubectl delete -f microservices/ --ignore-not-found=true
echo ""

# Delete infrastructure
print_info "Deleting infrastructure services..."
kubectl delete -f infrastructure/ --ignore-not-found=true
echo ""

# Delete config
print_info "Deleting configuration..."
kubectl delete -f config/ --ignore-not-found=true
echo ""

# Delete namespace (this will delete everything)
print_info "Deleting namespace neo4flix..."
kubectl delete namespace neo4flix --ignore-not-found=true
echo ""

print_info "====================================="
print_info "Cleanup Complete!"
print_info "====================================="
echo ""
print_info "All Neo4flix resources have been removed from Kubernetes."
echo ""
