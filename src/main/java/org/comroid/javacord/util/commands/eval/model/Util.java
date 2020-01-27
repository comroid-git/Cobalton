package org.comroid.javacord.util.commands.eval.model;

public class Util {
    public static String escapeString(String input) {
        return input
                // escape backticks so embeds won't print wrong
                .replaceAll("```", "´´´");
    }
}
