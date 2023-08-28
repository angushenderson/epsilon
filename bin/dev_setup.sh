### LOCAL MINIKUBE K8S CLUSTER INITIALIZATION ###
MINIKUBE_STATUS=$(minikube status | sed -n -e 's/^.*kubelet: //p')

if [[ $MINIKUBE_STATUS != 'Running' ]]
then
  minikube start
fi


### BUILD ###
mvn clean install
eval $(minikube -p minikube docker-env)
docker build -t com.angushenderson/epsilon-rest ./epsilon-rest
docker build -t com.angushenderson/epsilon-core ./epsilon-core


### DEPLOYMENT INTO MINIKUBE ###
minikube status

kubectl delete all --all --force --grace-period=0
kubectl delete all --all --force --grace-period=0
#kubectl delete -f ./kube/03-epsilon-rest.yaml --force
#kubectl delete -f ./kube/04-epsilon-rest.yaml --force
kubectl apply -f ./kube/05-epsilon-runtime-python-3.yaml
kubectl config set-context --current --namespace=epsilon
#kubectl port-forward deployment/kafka-broker 9092

kubectl get all
