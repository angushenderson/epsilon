package com.angushenderson;

import com.angushenderson.enums.RuntimeExecutionStatus;
import com.angushenderson.model.ExecutionJobRequest;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class WorkerPodJobExecutor {

  @Inject KubernetesClient client;
  @Inject WorkerPodObserver workerPodObserver;

  public String executeJobRequest(ExecutionJobRequest request, Pod pod) {
    log.info("Executing job {}", request.id());
//    workerPodObserver.removeAvailableWorkerPod(pod.getMetadata().getName());

    uploadFilesToPod(request, pod);
    log.info("Executing command: {}", request.generateCommand());
    String output = null;
    try {
      output = executeCommandOnPod(pod, "python3", "/runtime/main.py");
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      throw new RuntimeException(e);
    }
    updatePodExecutionStatus(pod, RuntimeExecutionStatus.COMPLETE);
    deletePod(pod);
    log.info("Command result: {}", output);
    return output;
  }

  private void uploadFilesToPod(ExecutionJobRequest request, Pod pod) {
    request
        .files()
        .forEach(
            encodedFile ->
                client
                    .pods()
                    .inNamespace(pod.getMetadata().getNamespace())
                    .withName(pod.getMetadata().getName())
                        // todo get better upload location
                    .file("/runtime/" + encodedFile.name())
                    .upload(new ByteArrayInputStream(encodedFile.content())));
  }

  private String executeCommandOnPod(Pod pod, String... cmd) throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<String> data = new CompletableFuture<>();
    try (ExecWatch execWatch = execCmd(pod, data, cmd)) {
      return data.get(15, TimeUnit.SECONDS);
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
              public void onFailure(Throwable t, Response failureResponse) {
                data.completeExceptionally(t);
              }

              @Override
              public void onClose(int i, String s) {
                data.complete(outputStream.toString());
              }
            })
        .exec(command);
  }

  private void updatePodExecutionStatus(Pod pod, RuntimeExecutionStatus value) {
    client
        .pods()
        .inNamespace("epsilon")
        .withName(pod.getMetadata().getName())
        .patch(
            PatchContext.of(PatchType.JSON_MERGE),
            "{\"metadata\":{\"annotations\":{\"execution_status\":\"" + value.toString() + "\"}}}");
  }

  private void deletePod(Pod pod) {
    client
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .delete();
  }
}
