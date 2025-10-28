# Neo4flix Kubernetes Deployment

Ce répertoire contient tous les manifests Kubernetes nécessaires pour déployer l'application Neo4flix localement.

## 📋 Prérequis

### 1. Cluster Kubernetes Local

Choisissez l'une des options suivantes pour exécuter Kubernetes localement :

#### Option A : Minikube (Recommandé)
```bash
# Installer Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Démarrer Minikube
minikube start --cpus=4 --memory=8192
```

#### Option B : Docker Desktop
- Activer Kubernetes dans Docker Desktop Settings > Kubernetes > Enable Kubernetes

#### Option C : Kind (Kubernetes in Docker)
```bash
# Installer Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Créer un cluster
kind create cluster --name neo4flix
```

### 2. kubectl

```bash
# Installer kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

### 3. Docker

Assurez-vous que Docker est installé et en cours d'exécution :
```bash
docker --version
```

## 🚀 Déploiement

### Étape 1 : Construire les images Docker

```bash
cd k8s
chmod +x build-images.sh
./build-images.sh
```

Ce script va :
- Compiler tous les microservices avec Maven
- Construire les images Docker pour chaque service
- Les taguer avec `neo4flix/<service>:latest`

### Étape 2 : Charger les images dans le cluster (si nécessaire)

**Pour Minikube :**
```bash
# Utiliser le daemon Docker de Minikube
eval $(minikube docker-env)
# Puis reconstruire les images
./build-images.sh
```

**Pour Kind :**
```bash
kind load docker-image neo4flix/user-service:latest --name neo4flix
kind load docker-image neo4flix/movie-service:latest --name neo4flix
kind load docker-image neo4flix/rating-service:latest --name neo4flix
kind load docker-image neo4flix/recommendation-service:latest --name neo4flix
kind load docker-image neo4flix/watchlist-service:latest --name neo4flix
kind load docker-image neo4flix/gateway-service:latest --name neo4flix
```

### Étape 3 : Déployer sur Kubernetes

```bash
chmod +x deploy.sh
./deploy.sh
```

Ce script va :
1. Créer le namespace `neo4flix`
2. Créer le ConfigMap avec toutes les configurations
3. Créer les PersistentVolumeClaims pour les services stateful
4. Déployer l'infrastructure (Zookeeper, Kafka, Redis, Neo4j)
5. Déployer tous les microservices
6. Attendre que tous les services soient prêts

## 📊 Structure du Projet

```
k8s/
├── config/
│   ├── namespace.yaml          # Namespace neo4flix
│   └── configmap.yaml          # Configuration centralisée
│
├── infrastructure/
│   ├── persistent-volumes.yaml # PVCs pour les données
│   ├── zookeeper.yaml          # Zookeeper deployment & service
│   ├── kafka.yaml              # Kafka deployment & service
│   ├── kafka-ui.yaml           # Kafka UI pour monitoring
│   ├── redis.yaml              # Redis deployment & service
│   └── neo4j.yaml              # Neo4j deployment & service
│
├── microservices/
│   ├── user-service.yaml       # User Service
│   ├── movie-service.yaml      # Movie Service
│   ├── rating-service.yaml     # Rating Service
│   ├── recommendation-service.yaml
│   ├── watchlist-service.yaml
│   └── gateway-service.yaml    # API Gateway
│
├── build-images.sh             # Script pour construire les images
├── deploy.sh                   # Script de déploiement
├── undeploy.sh                 # Script de nettoyage
└── README.md                   # Ce fichier
```

## 🔍 Vérification du Déploiement

### Vérifier le statut des pods

```bash
kubectl get pods -n neo4flix
```

Tous les pods doivent être en état `Running`.

### Vérifier les services

```bash
kubectl get services -n neo4flix
```

### Consulter les logs

```bash
# Logs d'un service spécifique
kubectl logs -f deployment/rating-service -n neo4flix

# Logs de tous les pods d'un service
kubectl logs -f -l app=rating-service -n neo4flix
```

## 🌐 Accéder aux Services

### Services Externes (via NodePort)

- **API Gateway**: http://localhost:30080
- **Neo4j Browser**: http://localhost:30474 (username: `neo4j`, password: `password`)
- **Kafka UI**: http://localhost:30091

### Port Forwarding (Alternative)

Si les NodePorts ne fonctionnent pas, utilisez le port forwarding :

```bash
# Gateway
kubectl port-forward service/gateway-service 9080:9080 -n neo4flix

# Neo4j
kubectl port-forward service/neo4j-service 7474:7474 7687:7687 -n neo4flix

# Kafka UI
kubectl port-forward service/kafka-ui-service 9091:8080 -n neo4flix
```

## 📈 Scaling

### Scaler un microservice

```bash
# Augmenter le nombre de replicas
kubectl scale deployment/rating-service --replicas=3 -n neo4flix

# Vérifier
kubectl get pods -n neo4flix | grep rating-service
```

### Auto-scaling (HPA)

```bash
# Activer l'autoscaling basé sur CPU
kubectl autoscale deployment rating-service \
  --cpu-percent=70 \
  --min=1 \
  --max=5 \
  -n neo4flix
```

## 🔧 Configuration

### Modifier la configuration

Éditez le fichier `config/configmap.yaml` puis :

```bash
kubectl apply -f config/configmap.yaml
kubectl rollout restart deployment -n neo4flix
```

### Variables d'environnement

Toutes les configurations sont centralisées dans le ConfigMap :
- Connexion Neo4j
- Connexion Kafka
- Connexion Redis
- URLs des services
- Secret JWT

## 🐛 Débogage

### Exécuter des commandes dans un pod

```bash
kubectl exec -it deployment/rating-service -n neo4flix -- /bin/bash
```

### Vérifier la connectivité Kafka

```bash
# Lister les topics Kafka
kubectl exec -it deployment/kafka -n neo4flix -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Vérifier les consumer groups
kubectl exec -it deployment/kafka -n neo4flix -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Vérifier Neo4j

```bash
# Tester la connexion Neo4j
kubectl exec -it deployment/neo4j -n neo4flix -- \
  cypher-shell -u neo4j -p password "MATCH (n) RETURN count(n);"
```

### Événements Kubernetes

```bash
# Voir les événements récents
kubectl get events -n neo4flix --sort-by='.lastTimestamp'
```

## 🔄 Mise à Jour

### Mettre à jour un service

```bash
# Reconstruire l'image
cd microservices/rating-service
mvn clean package -DskipTests
docker build -t neo4flix/rating-service:latest .

# Si Minikube, charger dans le daemon
eval $(minikube docker-env)

# Redémarrer le déploiement
kubectl rollout restart deployment/rating-service -n neo4flix

# Suivre le déploiement
kubectl rollout status deployment/rating-service -n neo4flix
```

## 🧹 Nettoyage

### Supprimer tous les services

```bash
./undeploy.sh
```

Ou manuellement :

```bash
kubectl delete namespace neo4flix
```

### Arrêter le cluster

**Minikube :**
```bash
minikube stop
# ou pour supprimer complètement
minikube delete
```

**Kind :**
```bash
kind delete cluster --name neo4flix
```

## 📦 Ressources Allouées

### Par Service

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| User Service | 250m | 500m | 512Mi | 1Gi |
| Movie Service | 250m | 500m | 512Mi | 1Gi |
| Rating Service | 250m | 500m | 512Mi | 1Gi |
| Recommendation | 250m | 500m | 512Mi | 1Gi |
| Watchlist | 250m | 500m | 512Mi | 1Gi |
| Gateway | 250m | 500m | 512Mi | 1Gi |
| Kafka | 500m | 1000m | 1Gi | 2Gi |
| Zookeeper | 250m | 500m | 512Mi | 1Gi |
| Neo4j | 500m | 1000m | 1Gi | 2Gi |
| Redis | 100m | 250m | 256Mi | 512Mi |

**Total Recommandé :**
- **CPU:** ~4 cores
- **Memory:** ~8-10 GB

## 🔐 Sécurité

### Secrets (TODO - À implémenter)

Pour la production, remplacez les valeurs sensibles du ConfigMap par des Secrets :

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: neo4flix-secrets
  namespace: neo4flix
type: Opaque
stringData:
  NEO4J_PASSWORD: "your-secure-password"
  JWT_SECRET: "your-jwt-secret"
```

## 📚 Ressources Additionnelles

- [Documentation Kubernetes](https://kubernetes.io/docs/)
- [Minikube Documentation](https://minikube.sigs.k8s.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)

## 🆘 Support

En cas de problème :

1. Vérifier les logs : `kubectl logs -f deployment/<service> -n neo4flix`
2. Vérifier les événements : `kubectl get events -n neo4flix`
3. Vérifier le statut : `kubectl describe pod <pod-name> -n neo4flix`
