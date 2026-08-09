package org.g5.util.cli;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLineTest {

    private static final String EXPECTED_HELP = """
    Usage: command: \n\t-a|--aOption: aOption description <A-OPTION>
    \t-b|--bOption: bOption description
    \t-c|--cOption: cOption description
    \t-d|--dOption: dOption description <D-OPTION>
    \t-e|--eOption: eOption description <E-OPTION>""";

    @Test
    void shouldParseCommandLineWithSimpleShortOpts() {
        String[] commandLine = List.of("command", "-a", "1", "-bcd", "2", "-e", "3")
                .toArray(new String[7]);

        CommandLine cli = setupTestCommand();

        @SuppressWarnings("rawtypes")
        List<Argument> arguments = cli.parse(commandLine);

        assertThat(arguments.size()).isEqualTo(5);

        System.out.println(cli.help());
        assertThat(cli.help()).isEqualTo(EXPECTED_HELP);
    }


    @Test
    void shouldHandleEmptyArgs() {
        CommandLine cli = setupTestCommand();

        String[] commandLine = new String[0];

        List<Argument> argumentList = cli.parse(commandLine);

        assertThat(argumentList).isEmpty();
    }

    private static CommandLine setupTestCommand() {
        CommandLine cli = new CommandLine("command");
        cli.addOption(new Option<>("a-option", "a", "aOption", "aOption description", Integer::parseInt));
        cli.addOption(new Option<>("b-option", "b", "bOption", "bOption description"));
        cli.addOption(new Option<>("c-option", "c", "cOption", "cOption description"));
        cli.addOption(new Option<>("d-option", "d", "dOption", "dOption description", Integer::parseInt));
        cli.addOption(new Option<>("e-option", "e", "eOption", "eOption description", Integer::parseInt));
        return cli;
    }
}