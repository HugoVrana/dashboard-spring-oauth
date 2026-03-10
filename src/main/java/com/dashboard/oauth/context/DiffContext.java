package com.dashboard.oauth.context;

import java.util.ArrayList;
import java.util.List;

public class DiffContext {

    private static final ThreadLocal<List<String>> DIFF_HOLDER = ThreadLocal.withInitial(ArrayList::new);

    public static void addDiff(String diff) {
        DIFF_HOLDER.get().add(diff);
    }

    public static String getDiff() {
        List<String> diffs = DIFF_HOLDER.get();
        if (diffs.isEmpty()) {
            return null;
        }
        return String.join(",", diffs);
    }

    public static void clear() {
        DIFF_HOLDER.remove();
    }
}

