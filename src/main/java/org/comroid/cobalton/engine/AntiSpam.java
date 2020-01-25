package org.comroid.cobalton.engine;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.comroid.cobalton.Bot;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.apache.logging.log4j.Level;
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

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageAuthor().isBotUser())
            return;
        if (event.isServerMessage() && !Bot.Prop.ENABLE_ANTISPAM.getValue(event.getServer().get()).asBoolean())
            return;

        final Message message = event.getMessage();

        for (SingleMessageScanner scanner : SingleMessageScanner.values()) {
            if (scanner.isSpam(message)) {
                logger.info(String.format("%s triggered AntiSpam Scanner: %s", message, scanner));

                // replace message
                message.delete("Antispam")
                        .thenCompose(nil -> event.getChannel().sendMessage(scanner.cleanup(message)))
                        .exceptionally(ExceptionLogger.get());
            }
        }
    }

    private enum SingleMessageScanner {
        CAPSLOCK(message -> {
            // count upper- and lowercase characters
            final String content = message.getReadableContent();

            long uppercaseCount = content.chars()
                    .filter(Character::isUpperCase)
                    .count();
            long lowercaseCount = content.chars()
                    .filter(Character::isLowerCase)
                    .count();

            return uppercaseCount >= (lowercaseCount * 2);
        }, (embed, message) -> embed.setDescription(message.getReadableContent().toLowerCase()));

        private final Predicate<Message> messagePredicate;
        private final BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner;

        SingleMessageScanner(Predicate<Message> messagePredicate, BiFunction<EmbedBuilder, Message, EmbedBuilder> cleaner) {
            this.messagePredicate = messagePredicate;
            this.cleaner = cleaner;
        }

        public boolean isSpam(Message message) {
            return messagePredicate.test(message);
        }

        public EmbedBuilder cleanup(Message message) {
            final EmbedBuilder embed = DefaultEmbedFactory.create()
                    .setFooter("Cobalton AntiSpam", Bot.API.getYourself().getAvatar().getUrl().toExternalForm())
                    .setAuthor(message.getAuthor())
                    .setTimestamp(message.getCreationTimestamp());

            return cleaner.apply(embed, message);
        }
    }
}
