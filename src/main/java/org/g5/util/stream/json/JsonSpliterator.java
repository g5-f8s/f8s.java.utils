package org.g5.util.stream.json;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * I am an {@link Iterable iterable} over a stream of JSON data items. Typically used to read large lists
 * containing rows of data items of interest.
 * I am stream friendly - and so offer the ability to go over really large volumes of data whilst maintaining
 * a very small memory footprint.
 * Performance is typically more than reasonable, as the bottle next is usually the underlying {@link InputStream input-stream}
 * which is usually remote, although performance is good even for data source from local file.
 */
public class JsonSpliterator implements Iterable<JsonNode> {

    private final String rootListNodeName;
    private final JsonParser jsonParser;
    private final Map<String, Object> metadata;
    private final boolean hasMetadata;

    private JsonNode currentNode;

    public static JsonSpliterator emptySpliterator(JsonMapper jsonParser) {
        return new JsonSpliterator(jsonParser, InputStream.nullInputStream());
    }

    public JsonSpliterator(JsonMapper jsonParser, InputStream inputStream) {
        this(null, jsonParser, inputStream);
    }

    public JsonSpliterator(String rootListNodeName, JsonMapper jsonParser, InputStream inputStream) {
        this(rootListNodeName, jsonParser, Collections.emptyMap(), false, inputStream);
    }

    public JsonSpliterator(String rootListNodeName, JsonMapper jsonParser,
                           Map<String, Object> metadata, boolean hasMetadata,
                           InputStream inputStream) {
        this.rootListNodeName = rootListNodeName;
        this.jsonParser = jsonParser.createParser(inputStream);
        this.metadata = metadata;
        this.hasMetadata = hasMetadata;
    }

    public Stream<JsonNode> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return new NodeIterator();
    }

    /**
     * The JSON Node iterator - iterates over data items of interest in the underlying data stream.
     */
    private final class NodeIterator implements Iterator<JsonNode> {

        private NodeIterator() {
            initialise();
        }

        @Override
        public boolean hasNext() {
            if (Objects.isNull(currentNode)) {
                currentNode = nextNode().orElse(null);
            }
            return Objects.nonNull(currentNode);
        }

        @Override
        public JsonNode next() {
            JsonNode next = Optional.ofNullable(currentNode).orElseThrow();
            currentNode = nextNode().orElse(null); //move to next node..
            return next;
        }

        /**
         * Moves to the next node in the list of interesting data items.
         * @return an {@link Optional optional} containing either the next {@link JsonNode JSON-node}, or empty.
         */
        private Optional<JsonNode> nextNode() {
            try {
                JsonToken nextToken = jsonParser.nextToken();
                if(Objects.isNull(nextToken) || jsonParser.isClosed()) {
                    return Optional.empty();
                }
                if (nextToken == JsonToken.END_ARRAY) {//we're done reading the list of interesting nodes...
                    JsonNode next = NullNode.instance;
                    for (nextToken = jsonParser.nextToken();
                         Objects.nonNull(nextToken) && nextToken != JsonToken.END_OBJECT;
                         nextToken = jsonParser.nextToken()) {
                        readMetadata();
                    }
                    return next instanceof NullNode ? Optional.empty() : Optional.ofNullable(next);
                } else if (nextToken == JsonToken.END_OBJECT) {
                    return Optional.empty();
                }
                return Optional.ofNullable(jsonParser.readValueAsTree());
            } catch (IOException ioe) {
                throw new IllegalStateException("Failed reading - broken JSON stream?", ioe);
            }
        }

        /**
         * Initialises the read on the underlying JSON data stream. Positions the json token pointer
         * at the correct point - the first data item of interest, in the list of interest.
         */
        private void initialise() {
            try {
                jsonParser.nextToken();//Start reading...
                if (notEmpty(rootListNodeName)) {
                    //while we're not at the list element we're interested in...
                    for(;jsonParser.hasCurrentToken(); jsonParser.nextToken()) {
                        if (rootListNodeName.equals(jsonParser.currentName())) {
                            break;
                        }
                        readMetadata();
                    }
                    if (rootListNodeName.equals(jsonParser.currentName())) {
                        JsonToken next = jsonParser.nextToken();
                        if (next == JsonToken.START_ARRAY) {
                            jsonParser.nextToken();//we should now be at the first object in the list of interest...
                        }
                    }
                } else {
                    if (jsonParser.currentToken() != JsonToken.START_ARRAY && jsonParser.nextToken() != JsonToken.START_ARRAY) {
                        throw new IllegalStateException("Can't parse this input! Does not start with the expected root element [%s], or an array!"
                                .formatted(rootListNodeName));
                    }
                    if (jsonParser.currentToken() != JsonToken.START_ARRAY && jsonParser.nextToken() == JsonToken.START_ARRAY) {
                        jsonParser.nextToken();
                    }
                }
            } catch (IOException ioe) {
                throw new IllegalArgumentException("Failed to initialise reading JSON stream! Caused by: " + ioe.getMessage() + ".", ioe);
            }
        }

        /**
         * Reads metadata around - either before or after - the list of interesting data items.
         * Useful for metadata that some systems may provide - for example, total expected rows, current page etc.
         *
         * @throws IOException if reading fails for any reason.
         */
        private void readMetadata() throws IOException {
            if (hasMetadata //IF: we're interested in metadata
                    && JsonToken.PROPERTY_NAME == jsonParser.currentToken()) {
                String fieldName = jsonParser.currentName();
                Object value;
                switch (jsonParser.nextToken()) {
                    case START_ARRAY -> value = jsonParser.readValueAs(List.class);
                    case VALUE_NULL -> value = null;
                    case VALUE_TRUE, VALUE_FALSE -> value = jsonParser.getBooleanValue();
                    case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> value = jsonParser.getNumberValue();
                    default -> value = jsonParser.getValueAsString();
                }
                if (Objects.nonNull(value)) {
                    metadata.put(fieldName, value);
                }
            }
        }
    }

    private static boolean isEmpty(String input) {
        return Objects.isNull(input) || input.isBlank();
    }

    private static boolean notEmpty(String input) {
        return !isEmpty(input);
    }
}
