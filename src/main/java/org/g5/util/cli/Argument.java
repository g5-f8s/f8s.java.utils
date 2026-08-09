package org.g5.util.cli;

public record Argument<T>(Option<T> option, T value) {
}
