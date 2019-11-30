package de.comroid.cobalton.command;

import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

@CommandGroup(name = "TextCommands", description = "Textual fun!")
public enum TextCommands {
    INSTANCE;

    private final static String[] EMOJI_TABLE = new String[]{
            "\uD83C\uDDE6", // a
            "\uD83C\uDDE7", // b
            "\uD83C\uDDE8", // c
            "\uD83C\uDDE9", // d
            "\uD83C\uDDEA", // e
            "\uD83C\uDDEB", // f
            "\uD83C\uDDEC", // g
            "\uD83C\uDDED", // h
            "\uD83C\uDDEE", // i
            "\uD83C\uDDEF", // j
            "\uD83C\uDDF0", // k
            "\uD83C\uDDF1", // l
            "\uD83C\uDDF2", // m
            "\uD83C\uDDF3", // n
            "\uD83C\uDDF4", // o
            "\uD83C\uDDF5", // p
            "\uD83C\uDDF6", // q
            "\uD83C\uDDF7", // r
            "\uD83C\uDDF8", // s
            "\uD83C\uDDF9", // t
            "\uD83C\uDDFA", // u
            "\uD83C\uDDFB", // v
            "\uD83C\uDDFC", // w
            "\uD83C\uDDFD", // x
            "\uD83C\uDDFE", // y
            "\uD83C\uDDFF" // z
    };

    @Command(convertStringResultsToEmbed = true)
    public String codebrackets() {
        return "```";
    }

    @Command(convertStringResultsToEmbed = true)
    public String lenny() {
        return "( ͡° ͜ʖ ͡°)";
    }

    @Command(
            description = "Emojify Text!",
            usage = "emojify <any string>",
            minimumArguments = 1,
            convertStringResultsToEmbed = true
    )
    public String emojify(String[] args) {
        final String str = String.join(" ", args).toLowerCase();
        StringBuilder yield = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                yield.append(' ');
                continue;
            }

            if (!Character.isAlphabetic(c))
                continue;

            yield.append(EMOJI_TABLE[c - 97])
                    .append(' ');
        }

        return yield.toString();
    }
}
