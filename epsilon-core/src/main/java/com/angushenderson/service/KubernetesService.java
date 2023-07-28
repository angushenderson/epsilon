package com.angushenderson.service;

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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Slf4j
public class KubernetesService {

    @EventListener(ApplicationReadyEvent.class)
    public void test() {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            SharedInformerFactory sharedInformerFactory = client.informers();
            SharedIndexInformer<Pod> podInformer = sharedInformerFactory.sharedIndexInformerFor(Pod.class, 30 * 1000L);
            log.info("Informer factory initialized.");
            // NOTE food for thought: do we keep a central note (db or thread safe equivelant centrally on core; or use "tags" on runtime containers to track state) - annotations in k8s (metadata)

            podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
                private boolean isRuntimeDeploymentPod(Pod pod) {
                    if (pod.getMetadata().getLabels().containsKey("app") && "epsilon".equals(pod.getMetadata().getNamespace())) {
                        return "epsilon-runtime-python-3".equals(pod.getMetadata().getLabels().get("app"));
                    }
                    return false;
                }

                private String updateMetadataAnnotation(String key, String value) {
                    return "{\"metadata\":{\"annotations\":{\"" + key + "\":\"" + value + "\"}}}";
                }

                @Override
                public void onAdd(Pod pod) {
                    // ingres will only occur once until this model then - all actions will be occured under update
                    if (isRuntimeDeploymentPod(pod)) {
                        log.info("Pod {}/{} got added", pod.getMetadata().getNamespace(), pod.getMetadata().getName());

                        client.pods().inNamespace("epsilon")
                                .withName(pod.getMetadata().getName())
                                .patch(PatchContext.of(PatchType.JSON_MERGE), updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.INITIALIZING.toString()));
                    }
                }

                @Override
                public void onUpdate(Pod oldPod, Pod newPod) {
                    if (isRuntimeDeploymentPod(newPod)) {
                        log.info("Pod {}/{} got updated", oldPod.getMetadata().getNamespace(), oldPod.getMetadata().getName());
                        log.info(newPod.getMetadata().getAnnotations().toString());

                        if (newPod.getStatus().getContainerStatuses().stream().filter(ContainerStatus::getReady).count() == 1 && "INITIALIZING".equals(newPod.getMetadata().getAnnotations().get("execution_status"))) {
                            client.pods().inNamespace("epsilon")
                                    .withName(newPod.getMetadata().getName())
                                    .patch(PatchContext.of(PatchType.JSON_MERGE), updateMetadataAnnotation("execution_status", RuntimeExecutionStatus.READY.toString()));
                        }
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
