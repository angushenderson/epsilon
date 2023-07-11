package com.angushenderson;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    @KafkaListener(topics = "jobs")
    public void processMessage(String content) {

    }

}
