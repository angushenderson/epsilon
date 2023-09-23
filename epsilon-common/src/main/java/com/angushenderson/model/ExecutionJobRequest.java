package com.angushenderson.model;

import java.util.List;

public record ExecutionJobRequest(
    String id, RuntimeEnvironment runtimeEnvironment, List<EncodedFile> files, String entrypoint) {

  public String generateCommand() {
    return "python3 ./runtime/" + entrypoint;
  }
}
