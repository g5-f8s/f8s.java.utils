package org.g5.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class InputStreamTokenIteratorTest {

    private static final Logger log = LoggerFactory.getLogger(InputStreamTokenIteratorTest.class);
    private static final int TEST_ID_LENGTH = 6;

    @Test
    public void shouldIterateOverTokensInInputStream() throws Exception {
        int numberOfDataItems = 10_000;
        String testData = generateTestData(TEST_ID_LENGTH, numberOfDataItems);
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testData.getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(testInputStream));

        InputStreamTokenIterator inputStreamTokenIterator = new InputStreamTokenIterator(reader, Pattern.compile("\\w"), ',');

        List<String> tokenList = StreamSupport.stream(Spliterators.spliteratorUnknownSize(inputStreamTokenIterator, Spliterator.ORDERED), false)
                .toList();

        log.info("Token-list: {}.", tokenList);
        assertThat(tokenList.size(), equalTo(numberOfDataItems));
        assertThat(tokenList, equalTo(Arrays.asList(testData.replaceAll("(\\[|\\])", "").split(","))));
    }

    @Test
    public void shouldIterateOverWords() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/text-data.txt"))));
        InputStreamTokenIterator inputStreamTokenIterator = new InputStreamTokenIterator(reader, Pattern.compile("(\\w|'|-|\\.)"));

        List<String> words = StreamSupport.stream(Spliterators.spliteratorUnknownSize(inputStreamTokenIterator, Spliterator.ORDERED), false)
                .toList();
        log.info("Token-list: {}.", words);
        assertThat(words, equalTo(expectedWords()));

        Map<String, Long> distinctWordCount = words.stream()
                .map(String::toLowerCase)
                .collect(Collectors.groupingBy(Function.identity(), //group-by distinct word
                        TreeMap::new,                               //...into a sorted Map
                        Collectors.counting()));                    //...counting occurrences of word
        log.info("\n{}", distinctWordCount.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue() + "\n").collect(Collectors.joining()));
    }

    @SuppressWarnings("all")
    private String generateTestData(int length, int count) {
        return "[" + IntStream.range(0, count).mapToObj(i -> RandomStringUtils.random(length, 50, 120, true, true))
                .collect(Collectors.joining(",")) + "]";
    }

    private static List<String> expectedWords() {
        return List.of("The",
                "first",
                "model",
                "of",
                "Aston",
                "Martin's",
                "second-century",
                "plan",
                "the",
                "DB11",
                "like",
                "its",
                "predecessor",
                "and",
                "its",
                "platform",
                "siblings",
                "incorporates",
                "aluminium",
                "extensively",
                "throughout",
                "its",
                "body",
                "Official",
                "manufacture",
                "of",
                "the",
                "DB11",
                "began",
                "at",
                "the",
                "Aston",
                "Martin",
                "facility",
                "in",
                "Gaydon",
                "Warwickshire",
                "in",
                "September",
                "2016",
                "Two",
                "engine",
                "configurations",
                "of",
                "the",
                "DB11",
                "were",
                "available",
                "a",
                "4.0-litre",
                "V8-engine",
                "model",
                "produced",
                "by",
                "Mercedes-AMG",
                "and",
                "a",
                "5.2-litre",
                "V12-engine",
                "model",
                "produced",
                "by",
                "Aston",
                "Martin",
                "The",
                "Volante",
                "version",
                "of",
                "the",
                "DB11",
                "was",
                "introduced",
                "in",
                "October",
                "2017",
                "In",
                "2018",
                "Aston",
                "Martin",
                "and",
                "its",
                "racing",
                "division",
                "replaced",
                "the",
                "DB11",
                "V12",
                "with",
                "the",
                "DB11",
                "V12",
                "AMR",
                "which",
                "brought",
                "an",
                "increased",
                "engine",
                "output",
                "The",
                "V8-powered",
                "model",
                "also",
                "received",
                "an",
                "enhancement",
                "in",
                "engine",
                "performance",
                "in",
                "2021"
        );
    }
}