package core.framework.test.redis;

import core.framework.redis.RedisSortedSet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author tempo
 */
public class MockRedisSortedSet implements RedisSortedSet {
    private final MockRedisStore store;

    public MockRedisSortedSet(MockRedisStore store) {
        this.store = store;
    }

    @Override
    public boolean add(String key, String value, long score, boolean onlyIfAbsent) {
        var sortedSet = store.putIfAbsent(key, new HashMap<>()).sortedSet();
        if (onlyIfAbsent) {
            return sortedSet.putIfAbsent(value, score) == null;
        } else {
            sortedSet.put(value, score);
            return true;
        }
    }

    @Override
    public Map<String, Long> range(String key, long start, long stop) {
        var value = store.get(key);
        if (value == null) return Map.of();
        Map<String, Long> sortedSet = value.sortedSet();
        int size = sortedSet.size();
        int startIndex = start < 0 ? 0 : (int) start;
        if (startIndex > size) startIndex = size;
        int endIndex = stop < 0 ? (int) stop + size : (int) stop;
        if (endIndex >= size) endIndex = size - 1;
        return sortedSet.entrySet().stream()
                .sorted(Entry.comparingByValue())
                .skip(startIndex)
                .limit(endIndex - startIndex + 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (key1, key2) -> key2, LinkedHashMap::new));
    }

    @Override
    public Map<String, Long> popByScore(String key, long minScore, long maxScore, long limit) {
        MockRedisStore.Value value = store.get(key);
        if (value == null) return Map.of();
        var sortedSet = value.sortedSet();
        return sortedSet.entrySet().stream()
                .filter(entry -> entry.getValue() >= minScore && entry.getValue() <= maxScore)
                .sorted(Entry.comparingByValue())
                .limit(limit)
                .peek(entry -> sortedSet.remove(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (key1, key2) -> key2, LinkedHashMap::new));
    }
}
