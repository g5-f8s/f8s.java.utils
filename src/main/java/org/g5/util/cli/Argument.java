package org.g5.util.cli;

public record Argument<T>(Option<T> option, T value) {

    public String shortOpt() {
        return option.shortOpt();
    }

    public String longOpt() {
        return option.longOpt();
    }
}
