package com.angushenderson.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CompletedExecutionJob implements Serializable {

  private String id;
  private String output;
}
