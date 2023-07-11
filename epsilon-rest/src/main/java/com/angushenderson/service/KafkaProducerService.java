package com.angushenderson.service;

import com.angushenderson.model.ExecutionJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, ExecutionJob> kafkaTemplate;

    public void send(ExecutionJob job) {
        kafkaTemplate.send("execution-job-topic", job);
    }

}
