package com.angushenderson;

import com.angushenderson.enums.RuntimeExecutionStatus;
import com.angushenderson.util.PodUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class WorkerPodObserver {

  private final Set<String> availableWorkerPods = ConcurrentHashMap.newKeySet();
//  private final Set<String> completedWorkerPods = ConcurrentHashMap.newKeySet();
  @Inject KubernetesClient client;

  public boolean isWorkerAvailable() {
    if (availableWorkerPods.size() > 0) {
      System.out.println(availableWorkerPods);
    }
    return availableWorkerPods.size() > 0;
  }

  public void removeAvailableWorkerPod(String podName) {
    availableWorkerPods.remove(podName);
//    completedWorkerPods.add(podName);
  }

  public Pod getAvailableWorkerPod() {
    String podName = availableWorkerPods.iterator().next();
    availableWorkerPods.remove(podName);

    return client.pods()
            .inNamespace("epsilon")
            .withName(podName)
            .get();
  }

  @Singleton
  ResourceEventHandler<Pod> podScheduler(SharedIndexInformer<Pod> podInformer) {
    return new ResourceEventHandler<Pod>() {
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

      private void deletePod(Pod pod) {
        client
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .delete();
      }

      @Override
      public void onAdd(Pod pod) {
        if (PodUtil.isNotRuntimeDeploymentPod(pod)) return;
        log.info(
            "Pod {}/{} got ADDED", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
        updatePodExecutionStatus(pod, RuntimeExecutionStatus.INITIALIZING);
      }

      @Override
      public void onUpdate(Pod oldPod, Pod newPod) {
        log.info("Pod {}/{} got UPDATED", newPod.getMetadata().getNamespace(), newPod.getMetadata().getName());
        if (PodUtil.isNotRuntimeDeploymentPod(newPod)) return;

        RuntimeExecutionStatus podStatus = PodUtil.getPodExecutionStatus(newPod);
        String podName = newPod.getMetadata().getName();

        if (PodUtil.isPodReady(newPod) && RuntimeExecutionStatus.INITIALIZING.equals(podStatus)) {
          updatePodExecutionStatus(newPod, RuntimeExecutionStatus.READY);
        } else if (RuntimeExecutionStatus.READY.equals(podStatus)) {
          log.info("POD READY TO GO");
          availableWorkerPods.add(podName);
        }
      }

      @Override
      public void onDelete(Pod pod, boolean b) {
        if (PodUtil.isNotRuntimeDeploymentPod(pod)) return;
        log.info(
            "Pod {}/{} got DELETED", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
//        completedWorkerPods.remove(pod.getMetadata().getName());
      }
    };
  }
}
