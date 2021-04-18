package ru.hilariousstartups.javaskills.checker.apitester;

import org.apache.commons.collections4.map.LRUMap;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Map;

@Repository
public class CallRepository {

    private final Map<Integer, ApiCallContext> cache = Collections.synchronizedMap(new LRUMap<>(1000));

    public void put(Integer id, ApiCallContext callContext) {
        cache.put(id, callContext);
    }

    public ApiCallContext get(Integer id) {
        return cache.get(id);
    }

}
