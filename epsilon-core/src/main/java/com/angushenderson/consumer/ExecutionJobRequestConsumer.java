package com.angushenderson.consumer;

import com.angushenderson.WorkerPodJobExecutor;
import com.angushenderson.WorkerPodObserver;
import com.angushenderson.model.CompletedExecutionJob;
import com.angushenderson.model.ExecutionJobRequest;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ExecutionJobRequestConsumer implements Runnable {

  private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
  @Inject ConnectionFactory connectionFactory;
  @Inject WorkerPodObserver workerPodObserver;
  @Inject WorkerPodJobExecutor workerPodJobExecutor;

  void onStart(@Observes StartupEvent ev) {
    scheduler.submit(this);
  }

  void onStop(@Observes ShutdownEvent ev) {
    scheduler.shutdown();
  }

  @Override
  public void run() {
    try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
      JMSConsumer consumer = context.createConsumer(context.createQueue("execution_job_requests"));
      log.info("Consumer listening: {}", consumer.toString());
      while (true) {
        if (!workerPodObserver.isWorkerAvailable()) continue;
        Message message = consumer.receive();
        if (message == null) return;
        log.info("PROCESSING MESSAGE {}", message.getJMSMessageID());
        log.info("doing something");
        ExecutionJobRequest request = message.getBody(ExecutionJobRequest.class);
        String output =
            workerPodJobExecutor.executeJobRequest(
                request, workerPodObserver.getAvailableWorkerPod());
        context
            .createProducer()
            .send(
                context.createQueue("completed_execution_jobs"),
                context.createObjectMessage(
                    new CompletedExecutionJob(message.getJMSMessageID(), output)));
      }
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }
}
