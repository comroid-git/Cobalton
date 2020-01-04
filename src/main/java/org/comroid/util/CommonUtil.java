package org.comroid.util;

public class CommonUtil {
    public static boolean range(int fromInclusive, int actual, int toExclusive) {
        return fromInclusive <= actual & actual < toExclusive;
    }
}
