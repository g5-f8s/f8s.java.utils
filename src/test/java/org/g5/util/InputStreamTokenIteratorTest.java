package org.g5.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class InputStreamTokenIteratorTest {

    @Test
    public void shouldIterateOverTokensInInputStream() throws Exception {
        int numberOfDataItems = 10_000;
        String testData = generateTestData(6, numberOfDataItems);
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testData.getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(testInputStream));

        InputStreamTokenIterator inputStreamTokenIterator = new InputStreamTokenIterator(reader, ',');

        List<String> tokenList = StreamSupport.stream(Spliterators.spliteratorUnknownSize(inputStreamTokenIterator, Spliterator.ORDERED), false)
                .toList();

        System.out.println(tokenList);
        assertThat(tokenList.size(), equalTo(numberOfDataItems));
        assertThat(tokenList, equalTo(Arrays.asList(testData.replaceAll("(\\[|\\])", "").split(","))));
    }

    private String generateTestData(int length, int count) {
        return "[" + IntStream.range(0, count).mapToObj(i -> RandomStringUtils.random(length, 50, 120, true, true))
                .collect(Collectors.joining(",")) + "]";
    }

}