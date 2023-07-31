package com.angushenderson.service;

import com.angushenderson.controller.PodCommandExecutionController;
import com.angushenderson.enums.RuntimeExecutionStatus;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Slf4j
public class KubernetesService {

    private final Set<String> runningPods = Collections.synchronizedSet(new HashSet<>());

    @EventListener(ApplicationReadyEvent.class)
    public void test() {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            SharedInformerFactory sharedInformerFactory = client.informers();
            SharedIndexInformer<Pod> podInformer = sharedInformerFactory.sharedIndexInformerFor(Pod.class, 30 * 1000L);
            log.info("Informer factory initialized.");
            // NOTE food for thought: do we keep a central note (db or thread safe equivelant centrally on core; or use "tags" on runtime containers to track state) - annotations in k8s (metadata)

            podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
                private boolean isNotRuntimeDeploymentPod(Pod pod) {
                    if (pod.getMetadata().getLabels().containsKey("app") && "epsilon".equals(pod.getMetadata().getNamespace())) {
                        return !"epsilon-runtime-python-3".equals(pod.getMetadata().getLabels().get("app"));
                    }
                    return true;
                }

                private String updateMetadataAnnotation(String key, RuntimeExecutionStatus value) {
                    return "{\"metadata\":{\"annotations\":{\"" + key + "\":\"" + value.toString() + "\"}}}";
                }

                @Override
                public void onAdd(Pod pod) {
                    // ingres will only occur once until this model then - all actions will be occured under update
                    if (isNotRuntimeDeploymentPod(pod)) return;

                    log.info("Pod {}/{} got added", pod.getMetadata().getNamespace(), pod.getMetadata().getName());

                    client.pods().inNamespace("epsilon")
                            .withName(pod.getMetadata().getName())
                            .patch(PatchContext.of(PatchType.JSON_MERGE), updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.INITIALIZING));
                }

                @Override
                public void onUpdate(Pod oldPod, Pod newPod) {
                    if (isNotRuntimeDeploymentPod(newPod)) return;

                    RuntimeExecutionStatus podStatus = RuntimeExecutionStatus.valueOf(newPod.getMetadata().getAnnotations().getOrDefault("execution_status", RuntimeExecutionStatus.UNKNOWN.name()));

//                    log.info("Pod {}/{} got updated", oldPod.getMetadata().getNamespace(), oldPod.getMetadata().getName());

                    if (newPod.getStatus().getContainerStatuses().stream().filter(ContainerStatus::getReady).count() == 1
                            && RuntimeExecutionStatus.INITIALIZING.equals(podStatus)) {
                        client.pods().inNamespace("epsilon")
                                .withName(newPod.getMetadata().getName())
                                .patch(PatchContext.of(PatchType.JSON_MERGE), updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.READY));
                    } else if (RuntimeExecutionStatus.READY.equals(podStatus) && !runningPods.contains(newPod.getMetadata().getName())) {
                        // todo need to be careful that this allocation/management is done sequentially - can't double allocate coz of threads
                        runningPods.add(newPod.getMetadata().getName());
                        updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.RUNNING);
                        String output = new PodCommandExecutionController(client).execCommandOnPod(newPod, "echo", "\"Hello World!\"");
                        log.info("Command output: {}", output);
                        updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.COMPLETE);
                        client.pods().inNamespace(newPod.getMetadata().getNamespace()).withName(newPod.getMetadata().getName()).delete();
                        runningPods.remove(newPod.getMetadata().getName());
                    }
                }

                @Override
                public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                    log.info("Pod {}/{} got deleted", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
                }
            });

            log.info("Starting all registered informers");
            Future<Void> startAllInformersFuture = sharedInformerFactory.startAllRegisteredInformers();
            startAllInformersFuture.get();

            // Wait for 1 minute
            Thread.sleep(60 * 60 * 1000L);
//            sharedInformerFactory.stopAllRegisteredInformers();
        } catch (ExecutionException executionException) {
            log.error("Error in starting all informers", executionException);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted ", e);
        }
    }
}
