package org.g5.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

/**
 * I an an {@link Iterator iterator} over an {@link java.io.InputStream input-stream}.
 * I provide the ability to iterate over tokenised data in an {@link java.io.InputStream input-stream}.
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

    private final BufferedReader inputStreamReader;
    private final Character token;
    private Character nextChar;
    private StringBuilder currentToken;

    public InputStreamTokenIterator(BufferedReader inputStreamReader, Character token) {
        this.inputStreamReader = inputStreamReader;
        this.token = token;
        this.nextChar = null;
        this.currentToken = new StringBuilder();
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
                if (canAccept(nextChar)) {
                    this.currentToken.append(nextChar);
                } else if (endOfTokenOrList()){
                    nextToken = this.currentToken.toString();
                    this.currentToken = new StringBuilder();
                    return nextToken;
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException();
        }
        return nextToken;
    }

    private boolean endOfTokenOrList() {
        return nextChar == token || nextChar == END_ARRAY;
    }

    private boolean canAccept(Character character) {
        return Character.isLetterOrDigit(character) && ! character.equals(token);
    }

}
