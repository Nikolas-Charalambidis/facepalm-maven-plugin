package dev.nichar.facepalm.engine;

import java.nio.file.Path;
import java.util.List;

import lombok.Value;


@Value
public class FileContext {
    Path path;

    String fullContent;

    List<String> lines;

    public String getLineOrEmpty(int index) {
        return (index >= 0 && index < lines.size()) ? lines.get(index) : "";
    }
}
