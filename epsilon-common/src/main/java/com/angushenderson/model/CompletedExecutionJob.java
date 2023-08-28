package com.angushenderson.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CompletedExecutionJob {

  public String id;

  public String stdout;

  public String stderr;
}
