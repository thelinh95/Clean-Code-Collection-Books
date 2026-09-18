package com.example.orderimport.importdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class IdLookup {

    private IdLookup() {
    }

    static Map<String, Long> toMap(List<Object[]> pairs) {
        Map<String, Long> map = HashMap.newHashMap(pairs.size());
        for (Object[] pair : pairs) {
            map.put((String) pair[0], (Long) pair[1]);
        }
        return map;
    }
}
