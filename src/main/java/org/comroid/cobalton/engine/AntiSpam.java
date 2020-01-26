package org.comroid.cobalton.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.comroid.cobalton.Bot;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

public enum AntiSpam implements MessageCreateListener {
    ENGINE;

    public final static Logger logger = LogManager.getLogger();
    /**
     * Gruber v2 URL Regex as posted on https://mathiasbynens.be/demo/url-regex#gruber_v2
     */
    public static final Pattern URL_PATTERN = Pattern.compile("((?s).*)(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www" +
            "\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^" +
            "\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))((?s).*)");

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageAuthor().isBotUser())
            return;
        //noinspection OptionalGetWithoutIsPresent
        if (event.isServerMessage() && !Bot.Prop.ENABLE_ANTISPAM.getValue(event.getServer().get()).asBoolean())
            return;

        final Message message = event.getMessage();
        final List<SpamRule> violated = new ArrayList<>(1);

        for (SpamRule spamRule : SpamRule.values())
            if (spamRule.isSpam(message))
                violated.add(spamRule);

        if (violated.size() == 0)
            return;

        final String[] content = {message.getContent()};

        violated.forEach(spamRule -> content[0] = spamRule.applyRule(content[0]));
        EmbedBuilder embed = generateEmbed(message, violated.toArray(SpamRule[]::new))
                .setDescription(content[0]);

        // replace message
        message.delete("AntiSpam")
                .thenCompose(nil -> event.getChannel().sendMessage(embed))
                .exceptionally(ExceptionLogger.get());
    }

    private EmbedBuilder generateEmbed(Message message, SpamRule[] spamRules) {
        return DefaultEmbedFactory.create()
                .setFooter(String.format("Cobalton AntiSpam • %s violated Rule%s: %s",
                        message.getAuthor().getDiscriminatedName(),
                        spamRules.length == 1 ? "" : 's',
                        spamRules.length == 1 ? spamRules[0] : Arrays.toString(spamRules)),
                        Bot.API.getYourself().getAvatar().getUrl().toExternalForm())
                //.setAuthor(message.getAuthor()) do not set an author; makes the embed smaller. author is visible in footer
                .setTimestamp(message.getCreationTimestamp());
    }

    private enum SpamRule {
        NoURLs(message -> URL_PATTERN.matcher(message.getContent()).matches(),
                content -> content.replaceAll(URL_PATTERN.pattern(), "$1[redacted]$7")),
        NoCaps(message -> {
            // count upper- and lowercase characters
            final String content = message.getReadableContent();

            long uppercaseCount = content.chars()
                    .filter(Character::isAlphabetic)
                    .filter(Character::isUpperCase)
                    .count();
            long lowercaseCount = content.chars()
                    .filter(Character::isAlphabetic)
                    .filter(Character::isLowerCase)
                    .count();

            return uppercaseCount >= (lowercaseCount * 2);
        }, String::toLowerCase);

        private final Predicate<Message> messagePredicate;
        private final Function<String, String> cleaner;

        SpamRule(Predicate<Message> messagePredicate, Function<String, String> cleaner) {
            this.messagePredicate = messagePredicate;
            this.cleaner = cleaner;
        }

        public boolean isSpam(Message message) {
            return messagePredicate.test(message);
        }

        public String applyRule(String content) {
            return cleaner.apply(content);
        }
    }
}
