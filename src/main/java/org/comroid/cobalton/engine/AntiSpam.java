package org.comroid.cobalton.engine;

import java.util.function.BiFunction;
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
    public static final Pattern URL_PATTERN = Pattern.compile("#(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))#iS");

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageAuthor().isBotUser())
            return;
        if (event.isServerMessage() && !Bot.Prop.ENABLE_ANTISPAM.getValue(event.getServer().get()).asBoolean())
            return;

        final Message message = event.getMessage();

        for (SpamRule spamRule : SpamRule.values()) {
            if (spamRule.isSpam(message)) {
                logger.info(String.format("%s triggered AntiSpam Scanner: %s", message, spamRule));

                // replace message
                message.delete("AntiSpam")
                        .thenCompose(nil -> event.getChannel().sendMessage(spamRule.cleanup(message, spamRule)))
                        .exceptionally(ExceptionLogger.get());
                break;
            }
        }
    }

    private enum SpamRule {
        NoURLs(message -> {
            // scan for any URL
            return URL_PATTERN.matcher(message.getContent()).matches();
        }, (embed, message) -> {
            // remove URL
            return embed.setDescription(message.getContent().replaceAll(URL_PATTERN.pattern(), "[redacted]"));
        }),
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
        }, (embed, message) -> embed.setDescription(message.getReadableContent().toLowerCase()));

        private final Predicate<Message> messagePredicate;
        private final BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner;

        SpamRule(Predicate<Message> messagePredicate, BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner) {
            this.messagePredicate = messagePredicate;
            this.cleaner = cleaner;
        }

        public boolean isSpam(Message message) {
            return messagePredicate.test(message);
        }

        public EmbedBuilder cleanup(Message message, SpamRule spamRule) {
            final EmbedBuilder embed = DefaultEmbedFactory.create()
                    .setFooter("Cobalton AntiSpam | Violated Rule: " + spamRule.name(), Bot.API.getYourself().getAvatar().getUrl().toExternalForm())
                    .setAuthor(message.getAuthor())
                    .setTimestamp(message.getCreationTimestamp());

            return cleaner.apply(embed, message);
        }
    }
}
