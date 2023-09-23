package com.angushenderson;

import com.angushenderson.model.CompletedExecutionJob;
import com.angushenderson.model.EncodedFile;
import com.angushenderson.model.ExecutionJobRequest;
import com.angushenderson.model.RuntimeEnvironment;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class ExecutionJobService {

    private final ListCommands<String, ExecutionJobRequest> commands;
    private final Multi<CompletedExecutionJob> stream;

    public ExecutionJobService(RedisDataSource dataSource, ReactiveRedisDataSource reactiveRedisDataSource) {
        commands = dataSource.list(ExecutionJobRequest.class);
        stream = reactiveRedisDataSource.pubsub(CompletedExecutionJob.class)
                .subscribe("completed-execution-jobs")
                .broadcast()
                .toAllSubscribers();
    }

    public ExecutionJobRequest submitExecutionJobRequest(RuntimeEnvironment runtimeEnvironment, String entrypoint, List<EncodedFile> files) {
        String id = UUID.randomUUID().toString();
        ExecutionJobRequest request = new ExecutionJobRequest(id, runtimeEnvironment, files, entrypoint);
        commands.lpush("execution-job-requests", request);
        log.info("Created job id {}", id);
        return request;
    }

    public Multi<CompletedExecutionJob> getCompletedExecutionJobs() {
        return stream;
    }
}
