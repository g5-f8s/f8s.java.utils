package org.g5.util.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.Map;

class YamlConfigMergeToolTest {

    private static final Logger log = LoggerFactory.getLogger(YamlConfigMergeToolTest.class);

    private static final ObjectMapper jsonParser = new ObjectMapper();

    @Test
    void shouldMergeYamls() throws Exception {
        Map<String, ?> merged = new YamlConfigMergeTool().merge("/yaml-test/sample.yaml", "/yaml-test/sample.overlay.yaml");
        StringWriter yamlWriter = new StringWriter();
        DumperOptions printOpts = new DumperOptions();
        printOpts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        printOpts.setPrettyFlow(true);
        new Yaml(printOpts).dump(merged, yamlWriter);
        log.info("merged yaml:\n{}", yamlWriter.toString());
        log.info(jsonParser.writerWithDefaultPrettyPrinter().writeValueAsString(merged));
    }

}