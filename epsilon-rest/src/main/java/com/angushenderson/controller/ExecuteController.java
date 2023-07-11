package com.angushenderson.controller;

import com.angushenderson.model.EncodedFile;
import com.angushenderson.model.ExecutionJob;
import com.angushenderson.model.RuntimeEnvironment;
import com.angushenderson.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class ExecuteController {

    @Autowired
    private KafkaProducerService kafkaProducer;

    @PostMapping(value = "/execute")
    public void execute() {
        kafkaProducer.send(new ExecutionJob(RuntimeEnvironment.PYTHON3,
                List.of(new EncodedFile("main.py", "print(\"Hello world!\")".getBytes())),
                "main.py"));
    }

}
