package org.comroid.cobalton.command;

import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.util.CommonUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.permission.PermissionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static String getReferencedContent(Message message, String[] args) {
        return (args.length == 0 ? message.getMessagesBefore(1)
                .thenApply(MessageSet::getNewestMessage)
                .join() // we don't want this to become asynchrounous
                .map(Message::getReadableContent) : Optional.<String>empty())
                .orElseGet(() -> String.join(" ", args).toLowerCase());
    }

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
            usage = "emojify [any string]",
            convertStringResultsToEmbed = true
    )
    public Object emojify(Message message, String[] args) {
        final String str = getReferencedContent(message, args);

        if (!str.matches(".*?[a-zA-Z0-9\\s]+.*?"))
            return DefaultEmbedFactory.create()
                    .addField("Error", "Input Input String must match Regular Expression: \n" +
                            "```regexp\n" +
                            ".*?[a-zA-Z0-9\\s]+.*?\n" +
                            "```")
                    .addField("Input String", "```\n" +
                            str + "\n" +
                            "```");

        StringBuilder yield = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                yield.append(' ');
                continue;
            }

            if (!Character.isAlphabetic(c))
                continue;

            if (CommonUtil.range(0, c - 'a', 26))
                yield.append(EMOJI_TABLE[c - 'a' /* 97 */])
                        .append(' ');
        }

        return yield.toString();
    }

    @Command(
            description = "Mock People!",
            usage = "mockify [any string]",
            convertStringResultsToEmbed = true
    )
    public Object mockify(Message message, String[] args) {
        final String str = getReferencedContent(message, args);

        if (!str.matches(".*?[a-zA-Z0-9\\s]+.*?"))
            return DefaultEmbedFactory.create()
                    .addField("Error", "Input Input String must match Regular Expression: \n" +
                            "```regexp\n" +
                            ".*?[a-zA-Z0-9\\s]+.*?\n" +
                            "```")
                    .addField("Input String", "```\n" +
                            str + "\n" +
                            "```");

        return IntStream.range(0, str.length())
                .mapToObj(val -> {
                    final char charAt = str.charAt(val);

                    if (Character.isWhitespace(charAt))
                        return ' ';

                    return (val % 2) != 1
                            ? Character.toUpperCase(charAt)
                            : Character.toLowerCase(charAt);
                })
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    @Command(
            description = "X-Word-Story Concluder",
            usage = "[each word count]",
            convertStringResultsToEmbed = true,
            maximumArguments = 1,
            async = true,
            useTypingIndicator = true,
            enablePrivateChat = false,
            requiredDiscordPermissions = PermissionType.MANAGE_MESSAGES
    )
    public Object concludeStory(ServerTextChannel stc, String[] args) {
        final List<String> yields = new ArrayList<>();

        final Optional<Message> stopship = stc.getMessagesAsStream()
                .limit(200)
                .filter(msg -> msg.getReadableContent().toLowerCase().contains("new story"))
                .findFirst();

        final String story = stopship.map(stc::getMessagesAfterAsStream)
                .orElseGet(() -> stc
                        .getMessagesAsStream()
                        .limit(100))
                .map(Message::getReadableContent)
                .filter(str -> !str.contains("concludeStory"))
                .filter(str -> str
                        .chars()
                        .filter(x -> x == ' ')
                        .count()
                        == (args.length == 0 ? 0
                        : Integer.parseInt(args[0])))
                .collect(Collectors.joining(" ", "```", "```"));

        return DefaultEmbedFactory.create(stc.getServer())
                .setTitle(String.format("The %s goes like this:", stopship
                        .map(message -> "story named " + message.getReadableContent())
                        .orElse("tale of unknown name")))
                .setDescription(story);
    }
}
