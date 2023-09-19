package com.angushenderson.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ExecutionJobRequest implements Serializable {

    private String id;

    private RuntimeEnvironment runtimeEnvironment;

    private List<EncodedFile> files;

    private String entrypoint;

    public String generateCommand() {
//        return "pwd";
        return "python3 ./runtime/" + entrypoint;
//        return "ls";
    }
}
