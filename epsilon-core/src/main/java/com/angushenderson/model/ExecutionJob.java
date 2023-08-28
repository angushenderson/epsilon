package com.angushenderson.model;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@RegisterForReflection
@AllArgsConstructor
@ToString
@Getter
public class ExecutionJob {

    private RuntimeEnvironment runtimeEnvironment;

    private List<EncodedFile> files;

    private String entrypoint;

}
