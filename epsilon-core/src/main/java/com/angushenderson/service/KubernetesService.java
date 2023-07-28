package com.angushenderson.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
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

            podInformer.addEventHandler(new ResourceEventHandler<>() {
                @Override
                public void onAdd(Pod pod) {
                    log.info("Pod {}/{} got added", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
                }

                @Override
                public void onUpdate(Pod oldPod, Pod newPod) {
                    log.info("Pod {}/{} got updated", oldPod.getMetadata().getNamespace(), oldPod.getMetadata().getName());
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
//            Thread.sleep(60 * 1000L);
//            sharedInformerFactory.stopAllRegisteredInformers();
        } catch (ExecutionException executionException) {
            log.error("Error in starting all informers", executionException);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted ", e);
        }
    }
}
