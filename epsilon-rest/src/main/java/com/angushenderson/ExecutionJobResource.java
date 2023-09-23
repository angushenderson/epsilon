package com.angushenderson;

import com.angushenderson.model.CompletedExecutionJob;
import com.angushenderson.model.EncodedFile;
import com.angushenderson.model.ExecutionJobRequest;
import com.angushenderson.model.RuntimeEnvironment;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/execute")
@Slf4j
public class ExecutionJobResource {

  @Inject
  ExecutionJobService executionJobService;

  @POST
  public ExecutionJobRequest submitExecutionJobRequest() {
    return executionJobService.submitExecutionJobRequest(
        RuntimeEnvironment.PYTHON3, "main.py", List.of(new EncodedFile("main.py", "print('Howdy!!')".getBytes())));
  }

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<CompletedExecutionJob> completedExecutionJobs() {
    return executionJobService.getCompletedExecutionJobs();
  }
}
