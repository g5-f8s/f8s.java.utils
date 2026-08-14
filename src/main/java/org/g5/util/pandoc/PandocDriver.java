package org.g5.util.pandoc;

import org.g5.util.cli.Argument;
import org.g5.util.cli.Arguments;
import org.g5.util.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class PandocDriver {

    private static final Logger log = LoggerFactory.getLogger(PandocDriver.class.getName());
    private static final String SHORT_OPT_SRC = "s";
    private static final String SHORT_OPT_OUTPUT = "o";
    private static final String SHORT_OPT_CSS = "c";
    private static final String SHORT_OPT_HDR = "h";
    private static final String SHORT_OPT_FTR = "f";
    private static final String SHORT_OPT_STANDALONE = "l";
    private static final String MD_FILE_EXT = "md";

    private final Arguments arguments;

    public PandocDriver(Arguments arguments) {
        this.arguments = arguments;
    }

    private Process process(File mdFile) throws IOException {
        String[] cmdArray = buildCommandArray(arguments, mdFile);
        log.trace("Built command: " + Arrays.toString(cmdArray));
        return Runtime.getRuntime().exec(cmdArray);
    }

    private String[] buildCommandArray(Arguments arguments, File mdFile) {
        return Stream.concat(Stream.of("pandoc"),
                        arguments.stream().map(pandocOptTranslator(mdFile))
                                .flatMap(Collection::stream)
                                .filter(not(String::isEmpty)))
                .toArray(String[]::new);
    }

    static void main(String[] args) throws IOException {
        CommandLine cmdLine = CommandLine.builder()
                .withCommand(PandocDriver.class.getSimpleName())
                .withStringArgOption("source-directory", SHORT_OPT_SRC, "source", "The source directory")
                .withStringArgOption("output-directory", SHORT_OPT_OUTPUT, "output", "The output directory")
                .withStringArgOption("style-sheet", SHORT_OPT_CSS, "css", "The style-sheet to use")
                .withStringArgOption("header-template", SHORT_OPT_HDR, "header", "The header template")
                .withStringArgOption("footer-template", SHORT_OPT_FTR, "footer", "The footer template")
                .withNoArgOption("stand-alone", SHORT_OPT_STANDALONE, "standalone", "Generate stand-alone files")
                .build();

        Arguments arguments = cmdLine.parse(args);

        String sourceDirUri = (String) arguments.get(SHORT_OPT_SRC).value();
        URI baseDirUri = Optional.ofNullable(sourceDirUri)
                .map(Paths::get)
                .map(Path::toUri)
                .orElseThrow(() -> new IllegalArgumentException("No dir found: %s".formatted(sourceDirUri)));
        String targetDir = (String) arguments.get(SHORT_OPT_OUTPUT).value();
        File targetFile = Optional.ofNullable(targetDir)
                .map(Paths::get)
                .map(Path::toFile).orElseThrow();
        targetFile.mkdirs();

        try (Stream<Path> dirListing = Files.list(Paths.get(baseDirUri))) {
            PandocDriver pandocDriver = new PandocDriver(arguments);
            Map<Integer, String> failureLog = new LinkedHashMap<>();//preserve order of insertion
            dirListing.filter(f -> f.getFileName().toString().endsWith(MD_FILE_EXT))
                    .map(Path::toFile)
                    .forEach(mdFile -> {
                        try {
                            Process p = pandocDriver.process(mdFile);
                            CompletableFuture<Process> processCompletableFuture = p.onExit();
                            processCompletableFuture.join();
                            if (execFailed(p)) {//exec failed - log error and continue
                                String errorMsg = String.format("pandoc failed with exit code: %d\n%s", p.exitValue(),
                                        new String(p.getErrorStream().readAllBytes()));
                                failureLog.put(p.exitValue(), errorMsg);
                                log.error(errorMsg);
                            }
                        } catch (IOException ioException) {
                            log.error("Failed to generate site: error was {}", ioException.getMessage(), ioException);
                        }
                    });
            if (failureLog.isEmpty()) {
                System.exit(0);
            } else {
                int lastErrorCode = logErrors(failureLog);
                System.exit(lastErrorCode);
            }
        }
    }

    private static int logErrors(Map<Integer, String> failureLog) {
        AtomicInteger statusCode = new AtomicInteger();
        failureLog.entrySet().stream()
                .map(e -> {
                    statusCode.set(e.getKey());
                    return e.getValue();
                })
                .forEach(log::error);
        return statusCode.get();
    }

    private static Function<Argument<?>, List<String>> pandocOptTranslator(File mdFile) {
        return arg -> {
            List<String> argLine = List.of("--" + arg.option().longOpt(),
                    Optional.ofNullable(arg.value()).map(Objects::toString).orElse(""));
            if ("s".equals(arg.option().shortOpt())) {
                return List.of(arg.value() + "/" + mdFile.getName());
            } else if ("o".equals(arg.option().shortOpt())) {
                return List.of("--" + arg.longOpt(),
                        arg.value() + "/" + mdFile.getName().replaceAll("\\.md", ".html"));
            }
            return argLine;
        };
    }

    private static boolean execFailed(Process p) {
        return p.exitValue() != 0;
    }
}