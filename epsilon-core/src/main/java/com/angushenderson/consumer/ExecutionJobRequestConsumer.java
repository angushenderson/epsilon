package com.angushenderson.consumer;

import com.angushenderson.WorkerPodJobExecutor;
import com.angushenderson.WorkerPodObserver;
import com.angushenderson.model.CompletedExecutionJob;
import com.angushenderson.model.ExecutionJobRequest;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ExecutionJobRequestConsumer implements Runnable {

  private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
  private final PubSubCommands<CompletedExecutionJob> publisher;
  private final ListCommands<String, ExecutionJobRequest> queue;
  @Inject WorkerPodObserver workerPodObserver;
  @Inject WorkerPodJobExecutor workerPodJobExecutor;

  public ExecutionJobRequestConsumer(RedisDataSource dataSource) {
    this.queue = dataSource.list(ExecutionJobRequest.class);
    this.publisher = dataSource.pubsub(CompletedExecutionJob.class);
  }

  void onStart(@Observes StartupEvent ev) {
    scheduler.submit(this);
  }

  void onStop(@Observes ShutdownEvent ev) {
    scheduler.shutdown();
  }

  @Override
  public void run() {
    log.info("Consumer listening");
    while (true) {
      if (!workerPodObserver.isWorkerAvailable()) continue;
      KeyValue<String, ExecutionJobRequest> item =
          queue.brpop(Duration.ofSeconds(1), "execution-job-requests");
      if (item == null) continue;
      ExecutionJobRequest request = item.value();
      log.info("Processing request {}", request.id());
      String output =
          workerPodJobExecutor.executeJobRequest(
              request, workerPodObserver.getAvailableWorkerPod());
      publisher.publish(
          "completed-execution-jobs", new CompletedExecutionJob(request.id(), output));
    }
  }
}
