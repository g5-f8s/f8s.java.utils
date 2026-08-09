package org.g5.util.cli;

import java.util.Objects;
import java.util.function.Function;

public record Option<T>(String name, String shortOpt, String longOpt, String description,
                        Function<String, T> valueConverter) {
    public Option(String name, String shortOpt, String longOpt, String description) {
        this(name, shortOpt, longOpt, description, null);
    }

    public T convert(String value) {
        if (requiresArgument()) {
            if (Objects.nonNull(value) && !value.isBlank()) {
                return valueConverter.apply(value);
            }
            throw new IllegalArgumentException("Option [%s|%s] requires a non-null value!".formatted(shortOpt, longOpt));
        }
        throw new IllegalArgumentException("Option [%s|%s] does not accept arguments!".formatted(shortOpt, longOpt));
    }

    public boolean requiresArgument() {
        return Objects.nonNull(valueConverter);
    }
}
