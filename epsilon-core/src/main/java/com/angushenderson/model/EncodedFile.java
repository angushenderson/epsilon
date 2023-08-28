package com.angushenderson.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@RegisterForReflection
@AllArgsConstructor
@ToString
@Getter
public class EncodedFile {

  private String name;

  private byte[] content;
}
