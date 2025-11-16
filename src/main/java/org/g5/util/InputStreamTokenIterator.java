package org.g5.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * I am an {@link Iterator iterator} over tokenised values read from a {@link Reader reader}.
 * I provide the ability to iterate over tokenised data - words - in a {@link Reader reader}.<br/><p/>
 * I support selector and delimiter patterns allowing flexibility in collecting values of interest into
 * coherent collections of characters. These could be words, identifiers or any other collection of characters
 * that make sense to the usage context.<br/><p/>
 * In conjunction with an appropriate {@link java.util.Spliterator spliterator}, I could be used to batch
 * these tokens and process these batches without waiting for the full stream to complete loading.
 * This can be powerful in terms of reducing latency for large data streams where there are a large
 * number of IDs (represented by the tokens in the underlying stream) to load.
 *
 * @author gerard.fernandes@gmail.com
 */
public class InputStreamTokenIterator implements Iterator<String> {

    private static final Character START_ARRAY = '[';
    private static final Character END_ARRAY = ']';

    private final Reader inputStreamReader;
    private final Predicate<Character> selector;
    private final Predicate<Character> excluder;
    private Character nextChar;
    private StringBuilder currentValue;

    public InputStreamTokenIterator(Reader inputStreamReader, Pattern selectorPattern, Character delimiter) {
        this(inputStreamReader, selectorPattern, Pattern.compile("(" + delimiter + "|\\" + END_ARRAY + "|\\" + START_ARRAY + ")"));
    }

    public InputStreamTokenIterator(Reader inputStreamReader, Pattern selectorPattern, Pattern delimiterPattern) {
        this.inputStreamReader = inputStreamReader;
        this.selector = characterSelector(selectorPattern);
        this.excluder = delimiterPatternExcluder(delimiterPattern);
        this.nextChar = null;
        this.currentValue = new StringBuilder();
    }

    @Override
    public boolean hasNext() {
        try {
            nextChar = (char) inputStreamReader.read();
            return Character.MAX_VALUE != this.nextChar;
        } catch (IOException ioException) {
            throw new RuntimeException();
        }
    }

    @Override
    public String next() {
        String nextToken = null;
        try {
            for (; nextChar != Character.MAX_VALUE; nextChar = (char) inputStreamReader.read()) {
                if (this.selector.test(nextChar)) {
                    this.currentValue.append(nextChar);
                } else if (this.excluder.test(nextChar)){
                    nextToken = this.currentValue.toString();
                    nextToken = nextToken.endsWith(".")? nextToken.substring(0, nextToken.length()-1) : nextToken;
                    this.currentValue = new StringBuilder();
                    if(nextToken.isEmpty()) {
                        continue;
                    }
                    return nextToken;
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException();
        }
        return nextToken;
    }

    private Predicate<Character> delimiterPatternExcluder(final Pattern tokenPattern) {
        return ch -> tokenPattern.matcher(Character.toString(ch)).matches();
    }

    private Predicate<Character> characterSelector(final Pattern selectorPattern) {
        return ch -> selectorPattern.matcher(Character.toString(ch)).matches();
    }

}
