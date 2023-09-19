package com.angushenderson.config;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ApplicationScoped
public class KubernetesInformerConfig {

    @Inject
    KubernetesClient client;

    @Singleton
    SharedInformerFactory sharedInformerFactory() {
        return client.informers();
    }

    @Singleton
    SharedIndexInformer<Pod> podInformer(SharedInformerFactory factory) {
        return factory.sharedIndexInformerFor(Pod.class, 30_000);
    }
}
