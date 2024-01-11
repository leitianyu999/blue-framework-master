package com.leitianyu.blue.web.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leitianyu
 * @date 2023/12/28
 */
public class MapUtils {

    @SafeVarargs
    public static <K, V> Map<K, V> createMap(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }

        List<AbstractMap.SimpleEntry<K, V>> entryList = new ArrayList<>();
        for (int i = 0; i < entries.length; i += 2) {
            K key = (K) entries[i];
            V value = (V) entries[i + 1];
            entryList.add(new AbstractMap.SimpleEntry<>(key, value));
        }

        return entryList.stream()
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

}
