package com.angushenderson.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ExecutionJob {

    private RuntimeEnvironment runtimeEnvironment;

    private List<EncodedFile> files;

    private String entrypoint;

}
