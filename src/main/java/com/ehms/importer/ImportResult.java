package com.ehms.importer;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ImportResult implements Serializable {
    private int total;
    private int imported;
    private int duplicates;
    private final List<String> errors = new ArrayList<>();

    void row() { total++; }
    void imported() { imported++; }
    void duplicate() { duplicates++; }
    void error(int line, String message) { errors.add("Line " + line + ": " + message); }
    public int getRejected() { return errors.size(); }
}
