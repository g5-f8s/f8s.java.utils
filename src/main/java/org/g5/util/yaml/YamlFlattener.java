package org.g5.util.yaml;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("unused")
public class YamlFlattener {

    public Properties toProperties(Map<String, ?> yaml) {
        Properties properties = new Properties();
        properties.putAll(toPropertiesMap(yaml));
        return properties;
    }

    public Map<String, String> toPropertiesMap(Map<String, ?> yaml) {
        return flatten(yaml);
    }

    private Map<String, String> flatten(Map<String, ?> yaml) {
        ImmutableMap.Builder<String, String> propertiesMapBuilder = ImmutableMap.builder();
        ImmutableSet.Builder<ConfigurationProperty<?>> pathBuilder = ImmutableSet.builder();
        for(Map.Entry<String, ?> diffEntry: yaml.entrySet()) {
            if(diffEntry.getValue() instanceof Map<?,?> subTreeDiff) {
                pathFrom(subTreeDiff.entrySet())
                        .forEach(pathElement -> pathBuilder.add(
                                new ConfigurationProperty<>(diffEntry.getKey() + "." + pathElement.path, pathElement.value())));
            } else {
                propertiesMapBuilder.put(diffEntry.getKey(), Optional.ofNullable(diffEntry.getValue())
                        .map(Object::toString)
                        .orElse(""));
            }
        }
        pathBuilder.build()
                .forEach(p -> propertiesMapBuilder.put(p.path(),
                        Optional.ofNullable(p.value()).map(Object::toString).orElse("")));
        return propertiesMapBuilder.build();
    }

    private Set<ConfigurationProperty<?>> pathFrom(Set<? extends Map.Entry<?, ?>> yamlSubtree) {
        ImmutableSet.Builder<ConfigurationProperty<?>> pathBuilder = ImmutableSet.builder();
        for(Map.Entry<?, ?> entry : yamlSubtree) {
            if (entry.getValue() instanceof Map<?, ?> nestedSubTree) {
                pathFrom(nestedSubTree.entrySet())
                        .forEach(pathElement -> pathBuilder.add(
                                new ConfigurationProperty<>(entry.getKey() + "." + pathElement.path(), pathElement.value())));
            } else {
                pathBuilder.add(ConfigurationProperty.from(entry));
            }
        }
        return pathBuilder.build();
    }


    public record ConfigurationProperty<T> (String path, T value){

        public static ConfigurationProperty<?> from(Map.Entry<?, ?> entry) {
            return new ConfigurationProperty<>((String) entry.getKey(), entry.getValue());
        }
    }
}
