package com.angushenderson.util;

import com.angushenderson.enums.RuntimeExecutionStatus;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

public class PodUtil {


    public static boolean isNotRuntimeDeploymentPod(Pod pod) {
        if (pod.getMetadata().getLabels().containsKey("app")
                && "epsilon".equals(pod.getMetadata().getNamespace())) {
            return !"epsilon-runtime-python-3".equals(pod.getMetadata().getLabels().get("app"));
        }
        return true;
    }

     public static RuntimeExecutionStatus getPodExecutionStatus(Pod pod) {
        return RuntimeExecutionStatus.valueOf(
                pod.getMetadata()
                        .getAnnotations()
                        .getOrDefault("execution_status", RuntimeExecutionStatus.UNKNOWN.name()));
    }

     public static boolean isPodReady(Pod pod) {
        return pod.getStatus().getContainerStatuses().stream()
                .filter(ContainerStatus::getReady)
                .count()
                == 1;
    }
}
