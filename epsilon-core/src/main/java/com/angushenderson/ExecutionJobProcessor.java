package com.angushenderson;

import com.angushenderson.model.CompletedExecutionJob;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
@Slf4j
public class ExecutionJobProcessor {

  @Incoming("requests")
  @Outgoing("completed_execution_jobs")
  @Blocking
  public CompletedExecutionJob process(String executionJobRequest) throws InterruptedException {
    log.info(executionJobRequest);
    Thread.sleep(200);
    return new CompletedExecutionJob(executionJobRequest, "Hello world!!", "");
  }
}
