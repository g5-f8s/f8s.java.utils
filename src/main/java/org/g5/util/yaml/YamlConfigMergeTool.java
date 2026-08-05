package org.g5.util.yaml;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class YamlConfigMergeTool {

    public Map<String, ?> merge(String source1, String source2) {
        InputStream yaml1InputStream = getClass().getResourceAsStream(source1);
        InputStream yaml2InputStream = getClass().getResourceAsStream(source2);

        return merge(yaml1InputStream, yaml2InputStream);
    }

    public Map<String, ?> merge(InputStream source1InputStream, InputStream source2InputStream) {

        Map<String, Object> source1Yaml = new Yaml().load(source1InputStream);
        Map<String, Object>  source2Yaml = new Yaml().load(source2InputStream);

        return merge(source1Yaml, source2Yaml);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> overlay) {
        LinkedHashMap<String, Object> partialMerge = new LinkedHashMap<>();
        //1. First pass: forward merge: take everything from base and for any match in overlay, replace with overlay..
        for (Map.Entry<String, Object> baseEntry: base.entrySet()) {
            if (baseEntry.getValue() instanceof Map subTree) {
                Map<String, Object> overlaySubtree = (Map<String, Object>) overlay.get(baseEntry.getKey());
                if (Objects.nonNull(overlaySubtree)) {
                    partialMerge.put(baseEntry.getKey(), merge((Map<String, Object>) subTree, overlaySubtree));
                } else {
                    partialMerge.put(baseEntry.getKey(), baseEntry.getValue());
                }
            } else {
                //if we're here, we're at a leaf node and we must choose the overlay, if it has an override, or the base value, if not.
                if (overlay.containsKey(baseEntry.getKey())) {
                    partialMerge.put(baseEntry.getKey(), overlay.get(baseEntry.getKey()));
                } else {
                    partialMerge.put(baseEntry.getKey(), baseEntry.getValue());
                }
            }
        }
        //2. Second pass: overlay merge: take everything from overlay, and overlay any entries in the overlay **NOT** in the base..
        for (Map.Entry<String, Object> overlayEntry: overlay.entrySet()) {
            //For the overlay processing, we will **only EVER** use Map.putIfAbsent(..) - this is intentional and avoids
            // overwriting _merged_ values from the previous run. This block is exclusively for configuration elements in
            // the overlay, that **do not exist** in the base configuration.
            if (overlayEntry.getValue() instanceof Map nestedOverlaySubtree) {
                partialMerge.putIfAbsent(overlayEntry.getKey(), merge((Map<String, Object>) nestedOverlaySubtree, nestedOverlaySubtree));
            } else {
                partialMerge.putIfAbsent(overlayEntry.getKey(), overlayEntry.getValue());
            }
        }
        return partialMerge;
    }

}
