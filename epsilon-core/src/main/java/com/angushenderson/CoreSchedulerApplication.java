package com.angushenderson;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusMain
public class CoreSchedulerApplication implements QuarkusApplication {

  @Inject KubernetesClient client;
  @Inject SharedInformerFactory sharedInformerFactory;
  @Inject ResourceEventHandler<Pod> podEventHandler;

  public static void main(String... args) {
    Quarkus.run(CoreSchedulerApplication.class, args);
  }

  @Override
  public int run(String... args) throws Exception {
    log.info("Hello I'm online...");
    try {
      client.pods().inNamespace("epsilon").withLabel("app", "epsilon-runtime-python-3").list();
    } catch (KubernetesClientException e) {
      log.error(e.getMessage());
      return 1;
    }

    sharedInformerFactory.startAllRegisteredInformers().get();
    final var podHandler = sharedInformerFactory.getExistingSharedIndexInformer(Pod.class);
    podHandler.addEventHandler(podEventHandler);
    Quarkus.waitForExit();
    return 0;
  }

  void onShutDown(@Observes ShutdownEvent event) {
    sharedInformerFactory.stopAllRegisteredInformers();
    client.pods()
            .inNamespace("epsilon")
            .withLabel("app", "epsilon-runtime-python-3")
            .withGracePeriod(0)
            .delete();
  }
}
