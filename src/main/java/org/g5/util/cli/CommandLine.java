package org.g5.util.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

/**
 * I am a minimal GNU style command-line parser. I do not currently support option groups.
 * <p>
 * I can understand GNU style options like <code>ls -lart</code> where the combined short-options may not
 * have arguments, except for the last one in the combined list. Long-options obviously cannot be combined and must
 * be each specified separately.
 * <p>
 * I support type conversion by leveraging Java lambda {@link Function function f(String) -> ?} allowing the user to setup value converters
 * per option, as desired. For example, a file URL may be translated directly to a file by specifying a
 * {@link Function function f(String) -> java.io.File} to validate and convert the option value.
 *
 * @author gerard.fernandes@gmail.com
 */
public class CommandLine {

    private static final Pattern SHORT_OPT_PATTERN = Pattern.compile("-([a-z]+)\\s+(.*?)(\\s|$)");
    private static final Pattern LONG_OPT_PATTERN = Pattern.compile("--([a-z]-)+\\s+(.*?)(\\s|$)");

    private final String commandName;
    private final List<Option<?>> options;

    public CommandLine(String commandName) {
        this.commandName = commandName;
        options = new ArrayList<>();
    }

    public  <T> void addOption(String name, String shortOpt, String longOpt, String description) {
        addOption(new Option<>(name, shortOpt, longOpt, description, null));
    }

    public  void addStringValueOption(String name, String shortOpt, String longOpt, String description) {
        addOption(new Option<>(name, shortOpt, longOpt, description, Function.identity()));

    }

    public  <T> void addOption(String name, String shortOpt, String longOpt, String description, Function<String, T> valueConverter) {
        addOption(new Option<>(name, shortOpt, longOpt, description, valueConverter));
    }

    public <T> void addOption(Option<T> option) {
        this.options.add(option);
    }

    @SuppressWarnings("rawtypes")
    public List<Argument> parse(String... cmdArgs) {
        String fullArgsLine = String.join(" ", cmdArgs);

        return Stream.concat(
                    handleShortOpts(fullArgsLine).stream(),
                    handleLongOpts(fullArgsLine).stream())
                .toList();
    }

    public String help() {
        return "Usage: " + commandName + ": " + optionsHelpString();
    }

    @Override
    public String toString() {
        return CommandLine.class.getSimpleName() + "{" +
                "commandName='" + commandName + '\'' +
                ", options=" + options +
                '}';
    }

    private String optionsHelpString() {
        return "\n\t" + options.stream()
                .map(o -> "-" + o.shortOpt() + "|--" + o.longOpt() + ": " + o.description() +
                        (o.requiresArgument()? " <"+ o.name().toUpperCase() +">" :""))
                .collect(Collectors.joining("\n\t"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Argument> handleLongOpts(String fullArgsLine) {
        Matcher longOptMatcher = LONG_OPT_PATTERN.matcher(fullArgsLine);
        List<Argument> parsedArguments = new ArrayList<>();
        while(longOptMatcher.find()) {
            String longOpt = longOptMatcher.group(1);
            String value = longOptMatcher.group(2);
            Option<?> option = getOption(longOpt);
            if (option.requiresArgument()) {
                parsedArguments.add(new Argument(option, option.convert(value)));
            } else {
                parsedArguments.add(new Argument(option, null));
            }
        }
        return unmodifiableList(parsedArguments);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Argument> handleShortOpts(String fullArgsLine) {
        Matcher shortOptMatcher = SHORT_OPT_PATTERN.matcher(fullArgsLine);
        List<Argument> parsedArgs = new ArrayList<>();
        while (shortOptMatcher.find()) {
            String possibleMultiOpt = shortOptMatcher.group(1);
            String value = shortOptMatcher.group(2);
            if (possibleMultiOpt.length() > 1) {
                char[] shortOpts = new char[possibleMultiOpt.length()];
                possibleMultiOpt.getChars(0, possibleMultiOpt.length(), shortOpts, 0);
                for (int i = 0; i < shortOpts.length; i++) {
                    String shortOpt = "" + shortOpts[i];
                    Option<?> option = getOption(shortOpt);
                    if (i == shortOpts.length - 1 && Objects.nonNull(value)) {
                        parsedArgs.add(new Argument(option, option.convert(value)));
                    } else {
                        parsedArgs.add(new Argument(option, null));
                    }
                }
            } else {
                Option<?> option = getOption(possibleMultiOpt);
                Argument<?> parsedArg;
                if (Objects.nonNull(value)) {
                    parsedArg = new Argument(option, option.convert(value));
                } else {
                    parsedArg = new Argument(option, null);
                }
                parsedArgs.add(parsedArg);
            }
        }
        return List.copyOf(parsedArgs);
    }

    private Option<?> getOption(String name) {
        Optional<Option<?>> option = this.options.stream()
                .filter(o -> o.shortOpt().equals(name) || o.longOpt().equals(name))
                .findFirst();
        return option.orElseThrow(() -> new IllegalArgumentException("I don't recognise option: %s!".formatted(name)));
    }

}
