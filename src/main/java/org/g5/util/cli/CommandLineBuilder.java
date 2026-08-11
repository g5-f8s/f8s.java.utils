package org.g5.util.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * I build instances of {@link CommandLine command-line}. I offer convenience methods
 * to add options to the command-line for parsing.
 *
 * @author gerard.fernandes@gmail.com
 */
public class CommandLineBuilder {

    private String command;
    private List<Option<?>> options = new ArrayList<>();
    private String description;

    protected CommandLineBuilder () {}

    public CommandLineBuilder withCommand(String command) {
        this.command = command;
        return this;
    }

    public <T> CommandLineBuilder withOption(Option<T> option) {
        this.options.add(option);
        return this;
    }

    public <T> CommandLineBuilder withNoArgOption(String name, String shortOpt, String longOpt, String description) {
        this.options.add(new Option<>(name, shortOpt, longOpt, description, null));
        return this;
    }

    public CommandLineBuilder withStringArgOption(String name, String shortOpt, String longOpt, String description) {
        this.options.add(new Option<>(name, shortOpt, longOpt, description, Function.identity()));
        return this;
    }

    public <T> CommandLineBuilder withOption(String name, String shortOpt, String longOpt, String description, Function<String, T> valueConverter) {
        this.options.add(new Option<>(name, shortOpt, longOpt, description, valueConverter));
        return this;
    }

    public CommandLineBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CommandLine build() {
        return new CommandLine(command, options, description);
    }

}
