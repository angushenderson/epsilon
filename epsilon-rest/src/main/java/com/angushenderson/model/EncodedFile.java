package com.angushenderson.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EncodedFile {

    private String name;

    private byte[] content;

}
