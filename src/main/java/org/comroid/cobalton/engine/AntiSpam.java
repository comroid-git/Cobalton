package org.comroid.cobalton.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.comroid.cobalton.Bot;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
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
        if (event.isPrivateMessage())
            return;

        final Server server = event.getServer().orElseThrow(AssertionError::new);

        if (!Bot.Prop.ENABLE_ANTISPAM.getValue(server).asBoolean())
            return;

        final Message message = event.getMessage();
        final List<SpamRule> violated = new ArrayList<>(1);

        for (SpamRule spamRule : SpamRule.collect(server))
            if (spamRule.isSpam(message))
                violated.add(spamRule);

        if (violated.size() == 0)
            return;

        // replace message
        final String[] content = {message.getContent()};

        if (content[0].length() > 2048 /* embed description length limit */)
            content[0] = "```Content was too long, could not post cleaned up content```";
        else violated.forEach(spamRule -> content[0] = spamRule.applyRule(content[0]));

        final SpamRule[] rulesArray = violated.toArray(SpamRule[]::new);
        final String reportMessage = String.format("%s violated SpamRule%s: %s",
                message.getAuthor().getDiscriminatedName(),
                rulesArray.length == 1 ? "" : 's',
                rulesArray.length == 1 ? rulesArray[0] : Arrays.toString(rulesArray));

        logger.log(SpamRule.INCIDENT, reportMessage);
        EmbedBuilder embed = DefaultEmbedFactory.create()
                .setFooter("Cobalton AntiSpam • " + reportMessage, Bot.API.getYourself().getAvatar().getUrl().toExternalForm())
                //removal .setAuthor(message.getAuthor()) do not set an author; makes the embed smaller. author is visible in footer
                //removal .setTimestamp(message.getCreationTimestamp()) do not add timestamp; it is repetitive information & makes the footer ugly
                .setDescription(content[0]);
        message.delete("AntiSpam")
                .thenCompose(nil -> event.getChannel().sendMessage(embed))
                .exceptionally(ExceptionLogger.get());
    }

    public enum SpamRule {
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

        public static Level INCIDENT = Level.forName("INCIDENT", Level.INFO.intLevel() - 50);
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

        public static Collection<SpamRule> collect(Server server) {
            final Collection<SpamRule> yields = new ArrayList<>();

            if (Bot.Prop.ANTISPAM_NOCAPS.getValue(server).asBoolean())
                yields.add(SpamRule.NoCaps);
            if (Bot.Prop.ANTISPAM_NOURLS.getValue(server).asBoolean())
                yields.add(SpamRule.NoURLs);

            return yields;
        }
    }
}
