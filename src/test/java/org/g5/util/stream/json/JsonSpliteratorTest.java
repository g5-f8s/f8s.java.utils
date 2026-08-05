package org.g5.util.stream.json;

import org.g5.util.NamedParameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class JsonSpliteratorTest {

    private static final JsonMapper jsonParser = JsonMapper.builder().build();

    @ParameterizedTest(name = "{index}. {0}")
    @MethodSource("testDataSource")
    void shouldStreamJson(NamedParameter<File> namedDataFile) throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        JsonSpliterator jsonSpliterator = new JsonSpliterator("data", jsonParser,
                metadata, true,
                new BufferedInputStream(new FileInputStream(namedDataFile.data())));

        List<JsonNode> jsonNodes = jsonSpliterator.stream().toList();

        assertThat(metadata.size(), equalTo(3));
        assertThat(jsonNodes.size(), equalTo(5));
        assertThat(metadata.get("rows"), equalTo(jsonNodes.size()));
    }

    @SuppressWarnings("all")
    public static Stream<NamedParameter<File>> testDataSource() throws URISyntaxException, IOException {
        URI dataFileDir = JsonSpliteratorTest.class.getResource("/json-spliterator").toURI();
        return StreamSupport.stream(Files.newDirectoryStream(Paths.get(dataFileDir)).spliterator(), false)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .map(Path::toFile)
                .map(NamedParameter.ofFile());
    }


}