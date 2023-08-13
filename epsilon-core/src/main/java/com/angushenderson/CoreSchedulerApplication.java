package com.angushenderson;

import com.angushenderson.enums.RuntimeExecutionStatus;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.*;
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

  @ApplicationScoped
  static final class CoreSchedulerApplicationConfig {

    private final Set<String> runningPods = ConcurrentHashMap.newKeySet();
    private final Set<String> completedPods = ConcurrentHashMap.newKeySet();
    @Inject KubernetesClient client;

    @Singleton
    SharedInformerFactory sharedInformerFactory() {
      return client.informers();
    }

    @Singleton
    SharedIndexInformer<Pod> podInformer(SharedInformerFactory factory) {
      return factory.sharedIndexInformerFor(Pod.class, 30_000);
    }

    @Singleton
    ResourceEventHandler<Pod> podScheduler(SharedIndexInformer<Pod> podInformer) {
      return new ResourceEventHandler<Pod>() {
        private boolean isNotRuntimeDeploymentPod(Pod pod) {
          if (pod.getMetadata().getLabels().containsKey("app")
              && "epsilon".equals(pod.getMetadata().getNamespace())) {
            return !"epsilon-runtime-python-3".equals(pod.getMetadata().getLabels().get("app"));
          }
          return true;
        }

        private void updatePodExecutionStatus(Pod pod, RuntimeExecutionStatus value) {
          client
              .pods()
              .inNamespace("epsilon")
              .withName(pod.getMetadata().getName())
              .patch(
                  PatchContext.of(PatchType.JSON_MERGE),
                  "{\"metadata\":{\"annotations\":{\"execution_status\":\""
                      + value.toString()
                      + "\"}}}");
        }

        private RuntimeExecutionStatus getPodExecutionStatus(Pod pod) {
          return RuntimeExecutionStatus.valueOf(
              pod.getMetadata()
                  .getAnnotations()
                  .getOrDefault("execution_status", RuntimeExecutionStatus.UNKNOWN.name()));
        }

        private boolean isPodReady(Pod pod) {
          return pod.getStatus().getContainerStatuses().stream()
                  .filter(ContainerStatus::getReady)
                  .count()
              == 1;
        }

        private void deletePod(Pod pod) {
          client.pods()
                  .inNamespace(pod.getMetadata().getNamespace())
                  .withName(pod.getMetadata().getName())
                  .delete();
        }

        @Override
        public void onAdd(Pod pod) {
          if (isNotRuntimeDeploymentPod(pod)) return;
          log.info(
              "Pod {}/{} got ADDED", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
          updatePodExecutionStatus(pod, RuntimeExecutionStatus.INITIALIZING);
        }

        @Override
        public void onUpdate(Pod oldPod, Pod newPod) {
          if (isNotRuntimeDeploymentPod(newPod)) return;

          RuntimeExecutionStatus podStatus = getPodExecutionStatus(newPod);
          String podName = newPod.getMetadata().getName();

          if (isPodReady(newPod) && RuntimeExecutionStatus.INITIALIZING.equals(podStatus)) {
            updatePodExecutionStatus(newPod, RuntimeExecutionStatus.READY);
          } else if (RuntimeExecutionStatus.READY.equals(podStatus)
              && !runningPods.contains(podName)) {
            runningPods.add(podName);
            updatePodExecutionStatus(newPod, RuntimeExecutionStatus.RUNNING);
            String output = execCommandOnPod(newPod, "echo", "\"Hello World!\"");
            log.info("Command output: {}", output);
            updatePodExecutionStatus(newPod, RuntimeExecutionStatus.COMPLETE);
            deletePod(newPod);
            runningPods.remove(podName);
            if (completedPods.contains(podName)) {
              log.error("SHIT double execution");
            }
            completedPods.add(podName);
          }
        }

        @Override
        public void onDelete(Pod pod, boolean b) {
          if (isNotRuntimeDeploymentPod(pod)) return;
          log.info(
              "Pod {}/{} got DELETED",
              pod.getMetadata().getNamespace(),
              pod.getMetadata().getName());
        }
      };
    }

    private String execCommandOnPod(Pod pod, String... cmd) {
      log.info(
          "Executing command: [{}] on pod [{}] in namespace [{}]",
          Arrays.toString(cmd),
          pod.getMetadata().getName(),
          pod.getMetadata().getNamespace());
      CompletableFuture<String> data = new CompletableFuture<>();
      try (ExecWatch execWatch = execCmd(pod, data, cmd)) {
        return data.get(15, TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private ExecWatch execCmd(Pod pod, CompletableFuture<String> data, String... command) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      return client
          .pods()
          .inNamespace(pod.getMetadata().getNamespace())
          .withName(pod.getMetadata().getName())
          .writingOutput(outputStream)
          .writingError(outputStream)
          .usingListener(
              new ExecListener() {
                @Override
                public void onOpen() {
                  log.info("Reading data...");
                }

                @Override
                public void onFailure(Throwable t, Response failureResponse) {
                  log.error(t.getMessage());
                  data.completeExceptionally(t);
                }

                @Override
                public void onClose(int code, String reason) {
                  log.info("Exit [{}]: {}", code, reason);
                  data.complete(outputStream.toString());
                }
              })
          .exec(command);
    }
  }
}
