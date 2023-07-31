package com.angushenderson.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class PodCommandExecutionController {

    private final KubernetesClient client;

    public PodCommandExecutionController(KubernetesClient client) {
        this.client = client;
    }

    public String execCommandOnPod(Pod pod, String... cmd) {
        log.info("Executing command: [{}] on pod [{}] in namespace [{}]", Arrays.toString(cmd), pod.getMetadata().getName(), pod.getMetadata().getNamespace());

        CompletableFuture<String> data = new CompletableFuture<>();
        try (ExecWatch execWatch = execCmd(pod, data, cmd)) {
//            log.error(execWatch.getError());
            return data.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ExecWatch execCmd(Pod pod, CompletableFuture<String> data, String... command) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        return client.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .writingOutput(outputStream)
                .writingError(outputStream)
                .usingListener(new SimpleListener(data, outputStream))
                .exec(command);
    }

    private record SimpleListener(CompletableFuture<String> data,
                                  ByteArrayOutputStream outputStream) implements ExecListener {

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
    }
}
