package org.g5.util;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.function.Function;

public record NamedParameter<T>(String name, T data) {
    @Override
    public @Nonnull String toString() {
        return name;
    }

    public static Function<File, NamedParameter<File>> ofFile() {
        return file -> new NamedParameter<File>(file.getName(), file);
    }
}
