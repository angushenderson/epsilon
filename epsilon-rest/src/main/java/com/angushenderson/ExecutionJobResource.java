package com.angushenderson;

import com.angushenderson.model.CompletedExecutionJob;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.UUID;

@Path("/execute")
@Slf4j
public class ExecutionJobResource {

    @Channel("execution_job_requests")
    Emitter<String> exeuctionJobEmitter;

    @Channel("completed_execution_jobs")
    Multi<CompletedExecutionJob> completedExecutionJobs;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String createRequest() {
        UUID uuid = UUID.randomUUID();
        exeuctionJobEmitter.send(uuid.toString());
        log.info(uuid.toString());
        return uuid.toString();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<CompletedExecutionJob> stream() {
        return completedExecutionJobs;
    }
}
