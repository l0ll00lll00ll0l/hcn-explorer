package com.hcn.v6;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

public class StateDumpTest {

    private IdentityHashMap<Object, String> names = new IdentityHashMap<>();
    private Map<String, Integer> counters = new HashMap<>();
    private Queue<Object> queue = new LinkedList<>();
    private List<String> output = new ArrayList<>();

    @Test
    void dumpStateAfter2Proofs() throws Exception {
        HcnGenerator gen = new HcnGenerator();
        gen.initialize();
        gen.proveNextSuperior();
        gen.proveNextSuperior();

        names.put(gen, "gen");
        queue.add(gen);

        while (!queue.isEmpty()) {
            Object obj = queue.poll();
            dumpObject(obj);
        }

        System.out.println("=== STATE DUMP AFTER 2 PROOFS ===");
        output.forEach(System.out::println);
        System.out.println("=== END DUMP (" + output.size() + " lines) ===");
    }

    private void dumpObject(Object obj) throws Exception {
        String name = names.get(obj);
        output.add("");
        output.add("--- " + name + " (" + obj.getClass().getSimpleName() + ") ---");

        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object val = f.get(obj);
            output.add("  " + f.getName() + " = " + formatAndEnqueue(val));
        }
    }

    private String formatAndEnqueue(Object val) {
        if (val == null) return "null";
        if (isSimple(val)) return val.toString();
        if (names.containsKey(val)) return names.get(val);

        if (val instanceof Collection) {
            Collection<?> coll = (Collection<?>) val;
            if (coll.isEmpty()) return "[]";
            List<String> items = new ArrayList<>();
            for (Object item : coll) {
                items.add(formatAndEnqueue(item));
            }
            return "[" + String.join(", ", items) + "]";
        }

        if (val instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) val;
            if (map.isEmpty()) return "{}";
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                entries.add(formatAndEnqueue(e.getKey()) + "=" + formatAndEnqueue(e.getValue()));
            }
            return "{" + String.join(", ", entries) + "}";
        }

        // new object — name it and enqueue
        String name = assignName(val);
        queue.add(val);
        return name;
    }

    private String assignName(Object obj) {
        if (names.containsKey(obj)) return names.get(obj);
        String prefix = obj.getClass().getSimpleName().toLowerCase();
        int count = counters.getOrDefault(prefix, 0);
        counters.put(prefix, count + 1);
        String name = prefix + "_" + count;
        names.put(obj, name);
        return name;
    }

    private boolean isSimple(Object obj) {
        return obj instanceof Number || obj instanceof String || obj instanceof Boolean
                || obj instanceof Character || obj.getClass().isEnum();
    }
}
