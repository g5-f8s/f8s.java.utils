package org.g5.util.cli;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Arguments extends AbstractCollection<Argument<?>> {

    private final Map<ArgumentKey, Argument<?>> argumentLookupMap;

    public Arguments(List<Argument<?>> arguments) {
        this.argumentLookupMap = arguments.stream()
                .collect(Collectors.toMap(argumentKeyBuilder(), Function.identity()));
    }

    public Argument<?> get(String shortOrLongOpt) {
        return findArgumentFor(shortOrLongOpt);
    }

    @Override
    public Iterator<Argument<?>> iterator() {
        return argumentLookupMap.values().iterator();
    }

    @Override
    public int size() {
        return argumentLookupMap.size();
    }

    private Argument<?> findArgumentFor(String shortOrLongOpt) {
        return this.argumentLookupMap.entrySet().stream()
                .filter(entry -> entry.getKey().shortOpt().equals(shortOrLongOpt) || entry.getKey().longOpt().equals(shortOrLongOpt))
                .findFirst()
                .orElseThrow()
                .getValue();
    }

    private static Function<Argument, ArgumentKey> argumentKeyBuilder() {
        return arg -> new ArgumentKey(arg.shortOpt(), arg.longOpt());
    }

    private record ArgumentKey(String shortOpt, String longOpt) {}
}
