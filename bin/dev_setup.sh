mvn clean install
eval $(minikube -p minikube docker-env)
docker build -t dev.angushenderson/epsilon-rest ./epsilon-rest
#minikube image load dev.angushenderson/epsilon-rest

minikube status

kubectl delete -f ./kube/03-epsilon-rest.yaml --force --grace-period=0
kubectl apply -f ./kube/
kubectl config set-context --current --namespace=epsilon

kubectl get all
