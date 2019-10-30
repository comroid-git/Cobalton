package de.kaleidox.util.eval;

public class Util {
    public static String escapeString(String input) {
        return input
                // escape backticks so embeds won't print wrong
                .replaceAll("```", "´´´");
    }
}
