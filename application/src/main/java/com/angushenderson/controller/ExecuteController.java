package com.angushenderson.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecuteController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping(value = "/execute")
    public void execute() {
        kafkaTemplate.send("jobs", "a message")
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        System.out.println("Sent message=[" + "a message" +
                                "] with offset=[" + result.getRecordMetadata().offset() + "]");
                    } else {
                        System.out.println("Unable to send message=[" +
                                "a message" + "] due to : " + exception.getMessage());
                    }
                });
    }

}
