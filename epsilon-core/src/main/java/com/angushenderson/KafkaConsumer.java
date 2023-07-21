package com.angushenderson;

import com.angushenderson.model.ExecutionJob;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    @KafkaListener(topics = "execution-job-topic")
    public void processMessage(ExecutionJob executionJob) {
        System.out.println(executionJob.getFiles().get(0).getContent());
    }

}
