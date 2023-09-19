package com.angushenderson;

import com.angushenderson.model.CompletedExecutionJob;
import com.angushenderson.model.EncodedFile;
import com.angushenderson.model.ExecutionJobRequest;
import com.angushenderson.model.RuntimeEnvironment;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;

@Path("/execute")
@Slf4j
public class ExecutionJobResource {

  @Channel("completed_execution_jobs")
  Multi<CompletedExecutionJob> completedExecutionJobs;

  @Inject ConnectionFactory connectionFactory;
// TODO - get data back to client (need to figure out pull vs push vs websocket etc
//  TODO - add isolation layer to containers with nestybox sysbox
// TODO - build out api after that, more languages - polish interfaces up etc
// TODO - could crete a cool ui
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String createRequest() {
    String uuid = UUID.randomUUID().toString(); // todo enforce uniqueness
    try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
      context
          .createProducer()
          .send(
              context.createQueue("execution_job_requests"),
              context.createObjectMessage(
                  new ExecutionJobRequest(
                      uuid,
                      RuntimeEnvironment.PYTHON3,
                      List.of(
                          new EncodedFile("main.py", "print('Genuinely hello world!!')".getBytes())),
                      "main.py")));
    }
    log.info(uuid);
    return uuid;
  }

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public Multi<CompletedExecutionJob> stream() {
    return completedExecutionJobs;
  }
}
