# 🚀 Guide de Démarrage Rapide - Neo4flix Kubernetes

## Déploiement en 3 Étapes

### Étape 1️⃣ : Démarrer un Cluster Kubernetes Local

Choisissez **UNE** des options suivantes :

#### Option A : Minikube (Recommandé)
```bash
minikube start --cpus=4 --memory=8192
```

#### Option B : Docker Desktop
Activer Kubernetes dans : Settings → Kubernetes → Enable Kubernetes

#### Option C : Kind
```bash
kind create cluster --name neo4flix
```

---

### Étape 2️⃣ : Construire les Images Docker

```bash
cd k8s
./build-images.sh
```

**Si vous utilisez Minikube**, exécutez d'abord :
```bash
eval $(minikube docker-env)
./build-images.sh
```

**Si vous utilisez Kind**, chargez les images :
```bash
kind load docker-image neo4flix/user-service:latest --name neo4flix
kind load docker-image neo4flix/movie-service:latest --name neo4flix
kind load docker-image neo4flix/rating-service:latest --name neo4flix
kind load docker-image neo4flix/recommendation-service:latest --name neo4flix
kind load docker-image neo4flix/watchlist-service:latest --name neo4flix
kind load docker-image neo4flix/gateway-service:latest --name neo4flix
```

---

### Étape 3️⃣ : Déployer l'Application

```bash
./deploy.sh
```

Le script va automatiquement :
- ✅ Créer le namespace `neo4flix`
- ✅ Créer les configurations
- ✅ Déployer l'infrastructure (Kafka, Neo4j, Redis)
- ✅ Déployer tous les microservices
- ✅ Attendre que tout soit prêt

---

## ✅ Vérification

```bash
# Vérifier que tous les pods sont en cours d'exécution
kubectl get pods -n neo4flix

# Attendre que tous soient "Running"
kubectl wait --for=condition=ready pod --all -n neo4flix --timeout=300s
```

---

## 🌐 Accéder aux Services

### Via NodePort (Par défaut)

| Service | URL |
|---------|-----|
| **API Gateway** | http://localhost:30080 |
| **Neo4j Browser** | http://localhost:30474 |
| **Kafka UI** | http://localhost:30091 |

**Credentials Neo4j :**
- Username: `neo4j`
- Password: `password`

### Via Port Forwarding (Alternative)

Si les NodePorts ne fonctionnent pas :

```bash
# Gateway API
kubectl port-forward service/gateway-service 9080:9080 -n neo4flix &

# Neo4j
kubectl port-forward service/neo4j-service 7474:7474 7687:7687 -n neo4flix &

# Kafka UI
kubectl port-forward service/kafka-ui-service 9091:8080 -n neo4flix &
```

Puis accédez à :
- Gateway: http://localhost:9080
- Neo4j: http://localhost:7474
- Kafka UI: http://localhost:9091

---

## 📊 Commandes Utiles

### Voir tous les services
```bash
kubectl get all -n neo4flix
```

### Voir les logs d'un service
```bash
kubectl logs -f deployment/rating-service -n neo4flix
```

### Redémarrer un service
```bash
kubectl rollout restart deployment/rating-service -n neo4flix
```

### Scaler un service
```bash
kubectl scale deployment/rating-service --replicas=3 -n neo4flix
```

---

## 🧪 Tester l'API

### Enregistrer un utilisateur
```bash
curl -X POST http://localhost:30080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Se connecter
```bash
curl -X POST http://localhost:30080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

---

## 🔧 Dépannage

### Les pods ne démarrent pas

```bash
# Vérifier les événements
kubectl get events -n neo4flix --sort-by='.lastTimestamp'

# Décrire un pod problématique
kubectl describe pod <pod-name> -n neo4flix

# Voir les logs
kubectl logs <pod-name> -n neo4flix
```

### Erreur "ImagePullBackOff"

Cela signifie que l'image n'est pas disponible. Solutions :

**Pour Minikube :**
```bash
eval $(minikube docker-env)
./build-images.sh
```

**Pour Kind :**
```bash
# Charger les images dans Kind (voir Étape 2)
```

### Services inaccessibles

```bash
# Vérifier le statut des services
kubectl get svc -n neo4flix

# Utiliser port-forward comme alternative
kubectl port-forward service/gateway-service 9080:9080 -n neo4flix
```

---

## 🧹 Nettoyage

### Supprimer tous les services
```bash
./undeploy.sh
```

### Arrêter le cluster
```bash
# Minikube
minikube stop

# Kind
kind delete cluster --name neo4flix
```

---

## 📚 Documentation Complète

Pour plus de détails, voir [README.md](README.md)

---

## 🎯 Ordre de Déploiement (Fait Automatiquement)

Le script `deploy.sh` respecte cet ordre :

1. **Namespace** → Isoler les ressources
2. **ConfigMap** → Configuration centralisée
3. **PersistentVolumeClaims** → Stockage pour les données
4. **Zookeeper** → Coordination Kafka
5. **Kafka** → Message broker
6. **Redis** → Cache
7. **Neo4j** → Base de données graphe
8. **Kafka UI** → Interface de monitoring
9. **Microservices** → User, Movie, Rating, Recommendation, Watchlist
10. **Gateway** → Point d'entrée API

---

## ⚙️ Configuration

Toutes les configurations sont dans `k8s/config/configmap.yaml` :
- URLs des services
- Connexions aux bases de données
- Configuration Kafka
- Secret JWT (à remplacer en production!)

Pour modifier :
```bash
vim k8s/config/configmap.yaml
kubectl apply -f k8s/config/configmap.yaml
kubectl rollout restart deployment -n neo4flix
```

---

## 🔥 Problèmes Courants

### 1. Pas assez de ressources
```
Error: Insufficient memory/CPU
```
**Solution :** Augmentez les ressources du cluster
```bash
minikube start --cpus=4 --memory=8192
```

### 2. Les PVCs restent en "Pending"
```bash
kubectl get pvc -n neo4flix
```
**Solution :** Vérifiez que votre cluster supporte le provisioning dynamique ou utilisez `hostPath` pour les tests locaux.

### 3. Kafka ne démarre pas
**Solution :** Attendez que Zookeeper soit complètement prêt, puis redémarrez Kafka
```bash
kubectl rollout restart deployment/kafka -n neo4flix
```

---

## 📞 Support

En cas de problème persistant :

1. Vérifiez les logs : `kubectl logs -f deployment/<service> -n neo4flix`
2. Consultez les événements : `kubectl get events -n neo4flix`
3. Vérifiez la documentation complète dans [README.md](README.md)
