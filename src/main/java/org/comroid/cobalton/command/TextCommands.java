package org.comroid.cobalton.command;

import org.comroid.cobalton.Bot;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.util.CommonUtil;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.permission.PermissionType;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    @Deprecated
    private static String getReferencedContent(Message message, String[] args) {
        return (args.length == 0 ? message.getMessagesBefore(1)
                .thenApply(MessageSet::getNewestMessage)
                .join() // we don't want this to become asynchrounous
                .map(Message::getReadableContent) : Optional.<String>empty())
                .orElseGet(() -> String.join(" ", args).toLowerCase());
    }

    public static CompletableFuture<String> findMessageContentByToken(TextChannel context, String token) {
        if (token.matches("\\d+")) {
            // is numeric
            final long num = Long.parseLong(token);

            if (token.length() < 3) {
                // count back TOKEN messages

                return context.getMessages((int) num)
                        .thenApply(MessageSet::getOldestMessage)
                        .thenApply(opt -> {
                            if (opt.isEmpty())
                                throw new NoSuchElementException(String
                                        .format("Could not count back %s messages", token));
                            return opt.get();
                        })
                        .thenApply(TextCommands::getTextualMessageContent);
            } else {
                // get message with id == TOKEN

                return context.getMessageById(num)
                        .thenApply(TextCommands::getTextualMessageContent);
            }
        } else {
            // is plaintext
            return CompletableFuture.completedFuture(token);
        }
    }

    public static String getTextualMessageContent(Message of) {
        // prioritize message content
        final String content = of.getReadableContent();
        if (!content.isEmpty())
            return content;

        // try to get embed
        return of.getEmbeds()
                .stream()
                .map(TextCommands::getTextualEmbedContent)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("No textual message content found in message: %s", of)));
    }

    public static String getTextualEmbedContent(Embed embed) {
        // prioritize description
        return embed.getDescription()
                // else append fields
                .orElseGet(() -> embed.getFields()
                        .stream()
                        .map(field -> String.format("**%s**\n%s", field.getName(), field.getValue()))
                        .collect(Collectors.joining("\n\n")));
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
            maximumArguments = 0,
            useTypingIndicator = true,
            enablePrivateChat = false,
            requiredDiscordPermissions = PermissionType.MANAGE_MESSAGES
    )
    public void concludeStory() {
        Bot.WSE.concludeStory().join();
    }
}
