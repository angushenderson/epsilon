### LOCAL MINIKUBE K8S CLUSTER INITIALIZATION ###
MINIKUBE_STATUS=$(minikube status | sed -n -e 's/^.*kubelet: //p')

if [[ $MINIKUBE_STATUS != 'Running' ]]
then
  minikube start
fi


### BUILD ###
mvn clean install
eval $(minikube -p minikube docker-env)
docker build -t dev.angushenderson/epsilon-rest ./epsilon-rest


### DEPLOYMENT INTO MINIKUBE ###
minikube status

kubectl delete -f ./kube/03-epsilon-rest.yaml
kubectl apply -f ./kube/
kubectl config set-context --current --namespace=epsilon
#kubectl port-forward deployment/kafka-broker 9092

kubectl get all
